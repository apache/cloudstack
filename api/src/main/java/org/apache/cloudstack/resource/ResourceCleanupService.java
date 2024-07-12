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

package org.apache.cloudstack.resource;

import org.apache.cloudstack.api.command.admin.resource.PurgeExpungedResourcesCmd;
import org.apache.cloudstack.framework.config.ConfigKey;

import com.cloud.vm.VirtualMachine;

public interface ResourceCleanupService {
    int MINIMUM_EXPUNGED_RESOURCE_PURGE_JOB_DELAY_IN_SECONDS = 3 * 60;
    ConfigKey<Boolean> ExpungedResourcePurgeEnabled = new ConfigKey<>("Advanced", Boolean.class,
            "expunged.resources.purge.enabled", "false",
            "Whether to run a background task to purge the DB records of the expunged resources",
            false, ConfigKey.Scope.Global);
    ConfigKey<String> ExpungedResourcePurgeResources = new ConfigKey<>("Advanced", String.class,
            "expunged.resources.purge.resources", "",
            "A comma-separated list of resource types that will be considered by the background task " +
                    "to purge the DB records of the expunged resources. Currently only VirtualMachine is supported. " +
                    "An empty value will result in considering all resource types for purging",
            false, ConfigKey.Scope.Global);
    ConfigKey<Integer> ExpungedResourcesPurgeInterval = new ConfigKey<>("Advanced", Integer.class,
            "expunged.resources.purge.interval", "86400",
            "Interval (in seconds) for the background task to purge the DB records of the expunged resources",
            false);
    ConfigKey<Integer> ExpungedResourcesPurgeDelay = new ConfigKey<>("Advanced", Integer.class,
            "expunged.resources.purge.delay", "300",
            "Initial delay (in seconds) to start the background task to purge the DB records of the " +
                    "expunged resources task", false);
    ConfigKey<Integer> ExpungedResourcesPurgeBatchSize = new ConfigKey<>("Advanced", Integer.class,
            "expunged.resources.purge.batch.size", "50",
            "Batch size to be used during purging of the DB records of the expunged resources",
            true);
    ConfigKey<String> ExpungedResourcesPurgeStartTime = new ConfigKey<>("Advanced", String.class,
            "expunged.resources.purge.start.time", "",
            "Start time to be used by the background task to purge the DB records of the expunged " +
                    "resources. Use format \"yyyy-MM-dd\" or \"yyyy-MM-dd HH:mm:ss\"", true);
    ConfigKey<Integer> ExpungedResourcesPurgeKeepPastDays = new ConfigKey<>("Advanced", Integer.class,
            "expunged.resources.purge.keep.past.days", "30",
            "The number of days in the past from the execution time of the background task to purge " +
                    "the DB records of the expunged resources for which the expunged resources must not be purged. " +
                    "To enable purging DB records of the expunged resource till the execution of the background " +
                    "task, set the value to zero.", true);
    ConfigKey<Integer> ExpungedResourcePurgeJobDelay = new ConfigKey<>("Advanced", Integer.class,
            "expunged.resource.purge.job.delay",
            String.valueOf(MINIMUM_EXPUNGED_RESOURCE_PURGE_JOB_DELAY_IN_SECONDS),
            String.format("Delay (in seconds) to execute the purging of the DB records of an expunged resource " +
                            "initiated by the configuration in the offering. Minimum value should be %d seconds " +
                            "and if a lower value is set then the minimum value will be used",
                    MINIMUM_EXPUNGED_RESOURCE_PURGE_JOB_DELAY_IN_SECONDS),
            true);

    enum ResourceType {
        VirtualMachine
    }

    long purgeExpungedResources(PurgeExpungedResourcesCmd cmd);
    void purgeExpungedVmResourcesLaterIfNeeded(VirtualMachine vm);
}
