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

package org.apache.cloudstack.cluster;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.component.Manager;
import com.cloud.utils.concurrency.Scheduler;
import org.apache.cloudstack.api.command.admin.cluster.ExecuteClusterDrsPlanCmd;
import org.apache.cloudstack.api.command.admin.cluster.GenerateClusterDrsPlanCmd;
import org.apache.cloudstack.api.command.admin.cluster.ListClusterDrsPlanCmd;
import org.apache.cloudstack.api.response.ClusterDrsPlanMigrationResponse;
import org.apache.cloudstack.api.response.ClusterDrsPlanResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

public interface ClusterDrsService extends Manager, Configurable, Scheduler {

    ConfigKey<Integer> ClusterDrsEventsExpireInterval = new ConfigKey<>(Integer.class, "drs.events.expire.interval",
            ConfigKey.CATEGORY_ADVANCED, "30", "The interval in days after which the DRS events will be cleaned up.",
            false, ConfigKey.Scope.Global, null);

    ConfigKey<Boolean> ClusterDrsEnabled = new ConfigKey<>(Boolean.class, "drs.automatic.enable",
            ConfigKey.CATEGORY_ADVANCED, "false", "Enable/disable automatic DRS on a cluster.", true,
            ConfigKey.Scope.Cluster, null);

    ConfigKey<Integer> ClusterDrsInterval = new ConfigKey<>(Integer.class, "drs.automatic.interval",
            ConfigKey.CATEGORY_ADVANCED, "60",
            "The interval in minutes after which a periodic background thread will schedule DRS for a cluster.", true,
            ConfigKey.Scope.Cluster, null);

    ConfigKey<Double> ClusterDrsIterations = new ConfigKey<>(Double.class, "drs.iterations",
            ConfigKey.CATEGORY_ADVANCED, "0.2",
            "The maximum number of VM migrations to perform for DRS. This is defined as a percentage (as a value " +
                    "between 0 and 1) of total number of workloads. The minimum number of iterations per round will " +
                    "be 1 and maximum will be equal to the total number of workloads in a cluster.",
            true, ConfigKey.Scope.Cluster, null);

    ConfigKey<String> ClusterDrsAlgorithm = new ConfigKey<>(String.class, "drs.algorithm", ConfigKey.CATEGORY_ADVANCED,
            "condensed", "DRS algorithm to execute on the cluster.", true, ConfigKey.Scope.Cluster, null);

    ConfigKey<Integer> ClusterDrsLevel = new ConfigKey<>(Integer.class, "drs.level", ConfigKey.CATEGORY_ADVANCED, "5",
            "The level of DRS to perform on cluster. This determines the amount of imbalance allowed in the cluster. " +
                    "10 means no imbalance is allowed and 1 means some imbalance is allowed",
            true,
            ConfigKey.Scope.Cluster, null, null, null, null, null,
            ConfigKey.Kind.Select, "1,2,3,4,5,6,7,8,9,10");

    ConfigKey<String> ClusterDrsMetric = new ConfigKey<>(String.class, "drs.metric",
            ConfigKey.CATEGORY_ADVANCED, "cpu",
            "The cluster imbalance metric to use when considering the imbalance in cluster. Possible values are " +
                    "memory, cpu, or both (cpu AND memory crosses the threshold).",
            true, ConfigKey.Scope.Cluster, null, null, null, null, null, ConfigKey.Kind.Select,
            "memory,cpu,both");

    /**
     * Executes the DRS (Distributed Resource Scheduler) command.
     *
     * @param cmd
     *         the GenerateClusterDrsPlanCmd object containing the command parameters
     *
     * @return a SuccessResponse object indicating the success of the operation
     */
    ListResponse<ClusterDrsPlanMigrationResponse> generateDrsPlan(GenerateClusterDrsPlanCmd cmd);

    /**
     * Executes a DRS plan for a cluster.
     *
     * @param cmd the ExecuteClusterDrsPlanCmd object containing the ID of the cluster and the map of virtual
     *            machines to hosts
     * @return true if the DRS plan was executed successfully, false otherwise
     * @throws InvalidParameterValueException if there is already a plan in READY state for the cluster or if the
     * cluster cannot be found by ID
     */
    boolean executeDrsPlan(ExecuteClusterDrsPlanCmd cmd);

    /**
     * Lists DRS plans for a cluster or a specific plan.
     *
     * @param cmd the ListClusterDrsPlanCmd object containing the ID of the cluster or the ID of the plan
     * @return a ListResponse object containing a list of ClusterDrsPlanResponse objects and the total number of plans
     * @throws InvalidParameterValueException if both clusterId and planId are specified or if the cluster cannot be
     * found by ID
     */
    ListResponse<ClusterDrsPlanResponse> listDrsPlan(ListClusterDrsPlanCmd cmd);
}
