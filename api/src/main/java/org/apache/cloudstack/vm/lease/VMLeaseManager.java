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
import org.apache.cloudstack.framework.config.ConfigKey;

import java.util.List;

public interface VMLeaseManager extends Manager {

    int MAX_LEASE_DURATION_DAYS = 365_00; // 100 years

    enum ExpiryAction {
        STOP,
        DESTROY
    }

    enum LeaseActionExecution {
        PENDING,
        DISABLED,
        DONE,
        CANCELLED
    }

    ConfigKey<Boolean> InstanceLeaseEnabled = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Boolean.class,
            "instance.lease.enabled", "false", "Indicates whether to enable the Instance lease," +
            " will be applicable only on instances created after lease is enabled. Disabling the feature cancels lease on existing instances with lease." +
            " Re-enabling feature will not cause lease expiry actions on grandfathered instances",
            true, List.of(ConfigKey.Scope.Global));

    ConfigKey<Integer> InstanceLeaseSchedulerInterval = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Integer.class,
            "instance.lease.scheduler.interval", "3600", "VM Lease Scheduler interval in seconds",
            false, List.of(ConfigKey.Scope.Global));

    ConfigKey<Integer> InstanceLeaseExpiryEventSchedulerInterval = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Integer.class,
            "instance.lease.eventscheduler.interval", "86400", "Lease expiry event Scheduler interval in seconds",
            false, List.of(ConfigKey.Scope.Global));

    ConfigKey<Integer> InstanceLeaseExpiryEventDaysBefore = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Integer.class,
            "instance.lease.expiryevent.daysbefore", "7", "Indicates how many days in advance, expiry events will be created before expiry.",
            true, List.of(ConfigKey.Scope.Global));

    void onLeaseFeatureToggle();
}
