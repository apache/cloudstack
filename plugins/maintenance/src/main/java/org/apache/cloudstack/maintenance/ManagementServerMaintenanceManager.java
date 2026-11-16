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

package org.apache.cloudstack.maintenance;

import org.apache.cloudstack.api.command.CancelMaintenanceCmd;
import org.apache.cloudstack.api.command.CancelShutdownCmd;
import org.apache.cloudstack.api.command.PrepareForMaintenanceCmd;
import org.apache.cloudstack.api.command.PrepareForShutdownCmd;
import org.apache.cloudstack.api.command.ReadyForShutdownCmd;
import org.apache.cloudstack.api.command.TriggerShutdownCmd;
import org.apache.cloudstack.api.response.ManagementServerMaintenanceResponse;
import org.apache.cloudstack.framework.config.ConfigKey;

import com.cloud.cluster.ManagementServerHostVO;

public interface ManagementServerMaintenanceManager {
    int DEFAULT_MS_MAINTENANCE_TIMEOUT_IN_MINS = 60;

    ConfigKey<Integer> ManagementServerMaintenanceTimeoutInMins = new ConfigKey<>(Integer.class,
            "management.server.maintenance.timeout",
            "Advanced",
            String.valueOf(DEFAULT_MS_MAINTENANCE_TIMEOUT_IN_MINS),
            "Timeout (in mins) for the maintenance window for the management server, default: 60 mins.",
            true,
            ConfigKey.Scope.Global,
            null);

    ConfigKey<Boolean> ManagementServerMaintenanceIgnoreMaintenanceHosts = new ConfigKey<>(Boolean.class,
            "management.server.maintenance.ignore.maintenance.hosts",
            "Advanced",
            String.valueOf(Boolean.FALSE),
            "Host in Maintenance state can sometimes block Management Server to go to Maintenance; this setting skips Host(s) in Maintenance state during Management Server Maintenance, default: false.",
            true,
            ConfigKey.Scope.Global,
            null);

    void registerListener(ManagementServerMaintenanceListener listener);

    void unregisterListener(ManagementServerMaintenanceListener listener);

    void onPreparingForMaintenance();

    void onCancelPreparingForMaintenance();

    void onMaintenance();

    void onCancelMaintenance();

    // Returns the number of pending jobs for the given management server msids.
    // NOTE: This is the msid and NOT the id
    long countPendingJobs(Long... msIds);

    boolean isAsyncJobsEnabled();

    // Indicates whether a shutdown has been triggered on the current management server
    boolean isShutdownTriggered();

    // Indicates whether the current management server is preparing to shutdown
    boolean isPreparingForShutdown();

    // Triggers a shutdown on the current management server by not accepting any more async jobs and shutting down when there are no pending jobs
    void triggerShutdown();

    // Prepares the current management server to shutdown by not accepting any more async jobs
    void prepareForShutdown();

    // Cancels the shutdown on the current management server
    void cancelShutdown();

    // Indicates whether the current management server is preparing to maintenance
    boolean isPreparingForMaintenance();

    void resetMaintenanceParams();

    long getMaintenanceStartTime();

    String getLbAlgorithm();

    // Prepares the current management server for maintenance by migrating the agents and not accepting any more async jobs
    void prepareForMaintenance(String lbAlorithm, boolean forced);

    // Cancels maintenance of the current management server
    void cancelMaintenance();

    void cancelPreparingForMaintenance(ManagementServerHostVO msHost);

    void cancelWaitForPendingJobs();

    // Returns whether the any of the ms can be shut down and if a shutdown has been triggered on any running ms
    ManagementServerMaintenanceResponse readyForShutdown(ReadyForShutdownCmd cmd);

    // Prepares the specified management server to shutdown by not accepting any more async jobs
    ManagementServerMaintenanceResponse prepareForShutdown(PrepareForShutdownCmd cmd);

    // Cancels the shutdown on the specified management server
    ManagementServerMaintenanceResponse cancelShutdown(CancelShutdownCmd cmd);

    // Triggers a shutdown on the specified management server by not accepting any more async jobs and shutting down when there are no pending jobs
    ManagementServerMaintenanceResponse triggerShutdown(TriggerShutdownCmd cmd);

    // Prepares the specified management server to maintenance by migrating the agents and not accepting any more async jobs
    ManagementServerMaintenanceResponse prepareForMaintenance(PrepareForMaintenanceCmd cmd);

    // Cancels maintenance of the specified management server
    ManagementServerMaintenanceResponse cancelMaintenance(CancelMaintenanceCmd cmd);
}
