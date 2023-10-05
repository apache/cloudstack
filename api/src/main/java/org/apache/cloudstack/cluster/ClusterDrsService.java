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
import org.apache.cloudstack.api.response.ClusterDrsPlanResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

public interface ClusterDrsService extends Manager, Configurable, Scheduler {

    ConfigKey<Integer> ClusterDrsPlanExpireInterval = new ConfigKey<>(Integer.class, "drs.plan.expire.interval",
            ConfigKey.CATEGORY_ADVANCED, "30", "The interval in days after which the DRS events will be cleaned up.",
            false, ConfigKey.Scope.Global, null, "Expire interval for old DRS plans", null, null, null);

    ConfigKey<Boolean> ClusterDrsEnabled = new ConfigKey<>(Boolean.class, "drs.automatic.enable",
            ConfigKey.CATEGORY_ADVANCED, "false", "Enable/disable automatic DRS on a cluster.", true,
            ConfigKey.Scope.Cluster, null, "Enable automatic DRS", null, null, null);

    ConfigKey<Integer> ClusterDrsInterval = new ConfigKey<>(Integer.class, "drs.automatic.interval",
            ConfigKey.CATEGORY_ADVANCED, "60",
            "The interval in minutes after which a periodic background thread will schedule DRS for a cluster.", true,
            ConfigKey.Scope.Cluster, null, "Interval for Automatic DRS ", null, null, null);

    ConfigKey<Integer> ClusterDrsMaxMigrations = new ConfigKey<>(Integer.class, "drs.max.migrations",
            ConfigKey.CATEGORY_ADVANCED, "50",
            "Maximum number of live migrations in a DRS execution.",
            true, ConfigKey.Scope.Cluster, null, "Maximum number of migrations for DRS", null, null, null);

    ConfigKey<String> ClusterDrsAlgorithm = new ConfigKey<>(String.class, "drs.algorithm",
            ConfigKey.CATEGORY_ADVANCED, "balanced", "The DRS algorithm to be executed on the cluster. Possible values are condensed, balanced.",
            true, ConfigKey.Scope.Cluster, null, "DRS algorithm", null, null,
            null, ConfigKey.Kind.Select, "condensed,balanced");

    ConfigKey<Float> ClusterDrsImbalanceThreshold = new ConfigKey<>(Float.class, "drs.imbalance",
            ConfigKey.CATEGORY_ADVANCED, "0.4",
            "Value of imbalance allowed in the cluster. 1.0 means no imbalance is allowed and 0.0 means full imbalance is allowed",
            true, ConfigKey.Scope.Cluster, null, "DRS imbalance", null, null, null);

    ConfigKey<String> ClusterDrsMetric = new ConfigKey<>(String.class, "drs.metric", ConfigKey.CATEGORY_ADVANCED,
            "memory",
            "The allocated resource metric used to measure imbalance in a cluster. Possible values are memory, cpu.",
            true, ConfigKey.Scope.Cluster, null, "DRS metric", null, null, null, ConfigKey.Kind.Select,
            "memory,cpu");

    /**
     * Generate a DRS plan for a cluster and save it as per the parameters
     *
     * @param cmd
     *         the GenerateClusterDrsPlanCmd object containing the command parameters
     *
     * @return a ClusterDrsPlanResponse object containing information regarding the migrations
     */
    ClusterDrsPlanResponse generateDrsPlan(GenerateClusterDrsPlanCmd cmd);

    /**
     * Executes a DRS plan for a cluster.
     *
     * @param cmd
     *         the ExecuteClusterDrsPlanCmd object containing the ID of the cluster and the map of virtual
     *         machines to hosts
     *
     * @return ClusterDrsPlanResponse response object
     *
     * @throws InvalidParameterValueException
     *         if there is already a plan in READY or IN_PROGRESS state for the
     *         cluster or if the
     *         cluster cannot be found by ID
     */
    ClusterDrsPlanResponse executeDrsPlan(ExecuteClusterDrsPlanCmd cmd);

    /**
     * Lists DRS plans for a cluster or a specific plan.
     *
     * @param cmd
     *         the ListClusterDrsPlanCmd object containing the ID of the cluster or the ID of the plan
     *
     * @return a ListResponse object containing a list of ClusterDrsPlanResponse objects and the total number of plans
     *
     * @throws InvalidParameterValueException
     *         if both clusterId and planId are specified or if the cluster cannot be
     *         found by ID
     */
    ListResponse<ClusterDrsPlanResponse> listDrsPlan(ListClusterDrsPlanCmd cmd);
}
