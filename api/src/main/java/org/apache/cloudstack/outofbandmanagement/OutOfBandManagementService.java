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
package org.apache.cloudstack.outofbandmanagement;

import com.cloud.dc.DataCenter;
import com.cloud.host.Host;
import com.cloud.org.Cluster;
import com.google.common.collect.ImmutableMap;
import org.apache.cloudstack.api.response.OutOfBandManagementResponse;
import org.apache.cloudstack.framework.config.ConfigKey;

import java.util.List;

public interface OutOfBandManagementService {

    ConfigKey<Long> ActionTimeout = new ConfigKey<Long>("Advanced", Long.class, "outofbandmanagement.action.timeout", "60",
                    "The out of band management action timeout in seconds, configurable by cluster", true, ConfigKey.Scope.Cluster);

    ConfigKey<Integer> SyncThreadPoolSize = new ConfigKey<Integer>("Advanced", Integer.class, "outofbandmanagement.sync.poolsize", "50",
            "The out of band management background sync thread pool size", true, ConfigKey.Scope.Global);

    ConfigKey<Integer> OutOfBandManagementBackgroundTaskExecutionInterval = new ConfigKey<>("Advanced", Integer.class, "outofbandmanagement.background.task.execution.interval", "4",
            "The interval in seconds for the out of band management (OOBM) background task.", true);

    long getId();
    boolean isOutOfBandManagementEnabled(Host host);
    void submitBackgroundPowerSyncTask(Host host);
    boolean transitionPowerStateToDisabled(List<? extends Host> hosts);

    OutOfBandManagementResponse enableOutOfBandManagement(DataCenter zone);
    OutOfBandManagementResponse enableOutOfBandManagement(Cluster cluster);
    OutOfBandManagementResponse enableOutOfBandManagement(Host host);

    OutOfBandManagementResponse disableOutOfBandManagement(DataCenter zone);
    OutOfBandManagementResponse disableOutOfBandManagement(Cluster cluster);
    OutOfBandManagementResponse disableOutOfBandManagement(Host host);

    OutOfBandManagementResponse configure(Host host, ImmutableMap<OutOfBandManagement.Option, String> options);
    OutOfBandManagementResponse executePowerOperation(Host host, OutOfBandManagement.PowerOperation operation, Long timeout);
    OutOfBandManagementResponse changePassword(Host host, String password);
}
