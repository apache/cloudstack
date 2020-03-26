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
package org.apache.cloudstack.simple.drs;

import com.cloud.utils.component.PluggableService;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import java.util.List;

public interface SimpleDRSManager extends PluggableService, Configurable {

    ConfigKey<String> SimpleDRSProvider = new ConfigKey<>("Advanced", String.class,
            "simple.drs.provider",
            "host-vm",
            "The simple DRS provider plugin that is used for rebalancing cluster workload on " +
                    "resources. Expected format: resource-workload, such as: host-vm", true);

    ConfigKey<String> SimpleDRSRebalancingAlgorithm = new ConfigKey<>("Advanced", String.class,
            "simple.drs.rebalancing.algorithm",
            "balanced",
            "The simple DRS rebalancing algorithm plugin that is used. Possible values: balanced, condensed",
            true);

    ConfigKey<Boolean> SimpleDRSAutomaticEnable = new ConfigKey<>("Advanced", Boolean.class,
            "simple.drs.automatic.enable",
            "false",
            "Enable/disable automatic DRS on a cluster",
            true, ConfigKey.Scope.Cluster);

    ConfigKey<Integer> SimpleDRSAutomaticInterval = new ConfigKey<>("Advanced", Integer.class,
            "simple.drs.automatic.interval",
            "60",
            "The interval in minutes after which a periodic background thread will schedule DRS for a cluster",
            true, ConfigKey.Scope.Cluster);

    ConfigKey<Double> SimpleDRSIterations = new ConfigKey<>("Advanced", Double.class,
            "simple.drs.iterations",
            "0.2",
            "The maximum number of iterations in a DRS job defined as a percentage of total number of workloads. " +
                    "The minimum number of iterations per round is 1 and the maximum is the number of workloads in a cluster",
            true, ConfigKey.Scope.Cluster);

    ConfigKey<Double> SimpleDRSImbalanceThreshold = new ConfigKey<>("Advanced", Double.class,
            "simple.drs.imbalance.threshold",
            "0.5",
            "The cluster imbalance threshold that is compared with the standard deviation percentage for a cluster utilization metric. " +
                    "The value is a percentage in decimal format.",
            true, ConfigKey.Scope.Cluster);

    List<String> listProviderNames();

    void schedule(long clusterId);

    void schedule(SimpleDRSJobInfo info);

    void balanceCluster(SimpleDRSJobInfo info);

    /**
     * Retrieve the workloads to be rebalanced within the cluster
     */
    List<SimpleDRSWorkload> getWorkloadsToRebalance(long clusterId);

    /**
     * True if cluster needs DRS, false if not
     */
    boolean isClusterImbalanced(long clusterId);

    /**
     * Return a list of compatible resources where the workload can be moved wihin the cluster of resources.
     * The list is sorted highest to lowest on the
     */
    List<SimpleDRSResource> findCompatibleDestinationResourcesForWorkloadRebalnce(SimpleDRSWorkload workload);

    /**
     * Calculate (but not attemp) workload rebalance cost-benefit to a destination resource
     */
    SimpleDRSRebalance calculateWorkloadRebalanceCostBenefit(SimpleDRSWorkload workload, SimpleDRSResource destination);

    /**
     * Calculate cluster imbalance
     */
    double calculateClusterImbalance(long clusterId);

    /**
     * Perform workload rebalance to a destination resource
     */
    boolean performWorkloadRebalance(SimpleDRSWorkload workload, SimpleDRSResource destination);
}
