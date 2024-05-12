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
package com.cloud.network.as;

import org.apache.cloudstack.framework.config.ConfigKey;

public interface AutoScaleManager extends AutoScaleService {

    ConfigKey<Integer> AutoScaleStatsInterval = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Integer.class,
            "autoscale.stats.interval",
            "60",
            "The interval (in seconds) when VM auto scaling statistics are processed to determine and perform scale action. Less than 1 means disabled.",
            false);

    ConfigKey<Integer> AutoScaleStatsCleanupDelay = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Integer.class,
            "autoscale.stats.cleanup.delay",
            "7200",
            "Determines how long (in seconds) to wait before actually removing auto scaling statistics from database. The default value is 7200 (2 hours).",
            false);

    ConfigKey<Integer> AutoScaleStatsWorker = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Integer.class,
            "autoscale.stats.worker",
            "10",
            "The Number of worker threads to scan the autoscale vm groups.",
            false);

    void checkAutoScaleUser(Long autoscaleUserId, long accountId);

    boolean deleteAutoScaleVmGroupsByAccount(Long accountId);

    void cleanUpAutoScaleResources(Long accountId);

    void doScaleUp(long groupId, Integer numVm);

    void doScaleDown(long groupId);

    void checkAllAutoScaleVmGroups();

    void checkAutoScaleVmGroup(AutoScaleVmGroupVO asGroup);

    void checkIfVmActionAllowed(Long vmId);

    void removeVmFromVmGroup(Long vmId);

    String getNextVmHostName(AutoScaleVmGroupVO asGroup);

    void checkAutoScaleVmGroupName(String groupName);
}
