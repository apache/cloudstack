/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cloudstack.vm.lease;

import com.cloud.utils.component.Manager;
import com.cloud.utils.concurrency.Scheduler;
import org.apache.cloudstack.framework.config.ConfigKey;

import java.util.List;

public interface VMLeaseManager extends Manager, Scheduler {

    enum ExpiryAction {
        STOP,
        DESTROY
    }

    ConfigKey<Boolean> InstanceLeaseEnabled = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Boolean.class,
            "instance.lease.enabled", "false", "Indicates whether to enable the Instance Lease feature",
            true, List.of(ConfigKey.Scope.Global));

    ConfigKey<Long> InstanceLeaseDuration = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Long.class,
            "instance.lease.duration", "90", "The default lease duration in days for the instance",
            true, List.of(ConfigKey.Scope.Account, ConfigKey.Scope.Domain));

    ConfigKey<String> InstanceLeaseExpiryAction = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, String.class,
            "instance.lease.expiryaction", "stop", "Default action to be taken at instance lease expiry",
            true, List.of(ConfigKey.Scope.Account, ConfigKey.Scope.Domain));

    ConfigKey<Long> InstanceLeaseSchedulerInterval = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Long.class,
            "instance.lease.scheduler.interval", "60", "VM Lease Scheduler interval in seconds",
            true, List.of(ConfigKey.Scope.Global));

    ConfigKey<Long> InstanceLeaseAlertSchedule = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Long.class,
            "instance.lease.alertscheduler.interval", "3600", "Lease Alert Scheduler interval in seconds",
            true, List.of(ConfigKey.Scope.Global));

    ConfigKey<Long> InstanceLeaseAlertStartsAt = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Long.class,
            "instance.lease.alert.startat", "7", "Denotes remaining day at which alerting will start",
            true, List.of(ConfigKey.Scope.Global));

}
