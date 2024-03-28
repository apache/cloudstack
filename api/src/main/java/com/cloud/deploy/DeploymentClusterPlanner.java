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

import org.apache.cloudstack.framework.config.ConfigKey;

import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.vm.VirtualMachineProfile;

/**
 */
public interface DeploymentClusterPlanner extends DeploymentPlanner {

    static final String ClusterCPUCapacityDisableThresholdCK = "cluster.cpu.allocated.capacity.disablethreshold";
    static final String ClusterMemoryCapacityDisableThresholdCK = "cluster.memory.allocated.capacity.disablethreshold";
    static final String ClusterThresholdEnabledCK = "cluster.threshold.enabled";

    static final ConfigKey<Float> ClusterCPUCapacityDisableThreshold =
        new ConfigKey<Float>(
            Float.class,
            ClusterCPUCapacityDisableThresholdCK,
            "Alert",
            "0.85",
            "Percentage (as a value between 0 and 1) of cpu utilization above which allocators will disable using the cluster for low cpu available. Keep the corresponding notification threshold lower than this to be notified beforehand.",
            true, ConfigKey.Scope.Cluster, null);
    static final ConfigKey<Float> ClusterMemoryCapacityDisableThreshold =
        new ConfigKey<Float>(
            Float.class,
            ClusterMemoryCapacityDisableThresholdCK,
            "Alert",
            "0.85",
            "Percentage (as a value between 0 and 1) of memory utilization above which allocators will disable using the cluster for low memory available. Keep the corresponding notification threshold lower than this to be notified beforehand.",
            true, ConfigKey.Scope.Cluster, null);
    static final ConfigKey<Boolean> ClusterThresholdEnabled =
        new ConfigKey<Boolean>(
            "Advanced",
            Boolean.class,
            ClusterThresholdEnabledCK,
            "true",
            "Enable/Disable cluster thresholds. If disabled, an instance can start in a cluster even though the threshold may be crossed.",
            false,
            ConfigKey.Scope.Global);

    static final ConfigKey<String> VmAllocationAlgorithm = new ConfigKey<>(
            String.class,
            "vm.allocation.algorithm",
            "Advanced",
            "random",
            "Order in which hosts within a cluster will be considered for VM/volume allocation. The value can be 'random', 'firstfit', 'userdispersing', 'userconcentratedpod_random', 'userconcentratedpod_firstfit', or 'firstfitleastconsumed'.",
            true,
            ConfigKey.Scope.Global, null, null, null, null, null,
            ConfigKey.Kind.Select,
            "random,firstfit,userdispersing,userconcentratedpod_random,userconcentratedpod_firstfit,firstfitleastconsumed");

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
    List<Long> orderClusters(VirtualMachineProfile vm, DeploymentPlan plan, ExcludeList avoid) throws InsufficientServerCapacityException;

    PlannerResourceUsage getResourceUsage(VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid) throws InsufficientServerCapacityException;

}
