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

import javax.ejb.Local;

import com.cloud.deploy.DeploymentPlanner.AllocationAlgorithm;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.vm.UserVmVO;

@Local(value = {DeployPlannerSelector.class})
public class HypervisorVmPlannerSelector extends AbstractDeployPlannerSelector {
    @Override
    public String selectPlanner(UserVmVO vm) {
        if (vm.getHypervisorType() != HypervisorType.BareMetal) {
            //check the allocation strategy
            if (_allocationAlgorithm != null) {
                if (_allocationAlgorithm.equals(AllocationAlgorithm.random.toString())
                        || _allocationAlgorithm.equals(AllocationAlgorithm.firstfit.toString())) {
                    return "FirstFitPlanner";
                } else if (_allocationAlgorithm.equals(AllocationAlgorithm.userdispersing.toString())) {
                    return "UserDispersingPlanner";
                } else if (_allocationAlgorithm.equals(AllocationAlgorithm.userconcentratedpod_random.toString())
                        || _allocationAlgorithm.equals(AllocationAlgorithm.userconcentratedpod_firstfit.toString())) {
                    return "UserConcentratedPodPlanner";
                }
            }
        }

        return null;
    }
}
