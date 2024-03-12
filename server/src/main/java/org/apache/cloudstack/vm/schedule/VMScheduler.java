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
package org.apache.cloudstack.vm.schedule;

import com.cloud.utils.component.Manager;
import com.cloud.utils.concurrency.Scheduler;
import org.apache.cloudstack.framework.config.ConfigKey;

import java.util.Date;
import java.util.List;

public interface VMScheduler extends Manager, Scheduler {
    ConfigKey<Integer> VMScheduledJobExpireInterval = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Integer.class, "vmscheduler.jobs.expire.interval", "30", "VM Scheduler expire interval in days", true);

    void removeScheduledJobs(List<Long> vmScheduleIds);

    void updateScheduledJob(VMScheduleVO vmSchedule);

    Date scheduleNextJob(VMScheduleVO vmSchedule, Date timestamp);
}
