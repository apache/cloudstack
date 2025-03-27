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

    enum ExpiryAction {
        STOP,
        DESTROY
    }

    ConfigKey<Long> InstanceLeaseSchedulerInterval = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Long.class,
            "instance.lease.scheduler.interval", "3600", "VM Lease Scheduler interval in seconds",
            true, List.of(ConfigKey.Scope.Global));

    ConfigKey<Long> InstanceLeaseAlertSchedule = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Long.class,
            "instance.lease.alertscheduler.interval", "86400", "Lease Alert Scheduler interval in seconds",
            true, List.of(ConfigKey.Scope.Global));

    ConfigKey<Long> InstanceLeaseExpiryAlertDaysBefore = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Long.class,
            "instance.lease.alert.daysbefore", "7", "Indicates how many days in advance the alert will be triggered before expiry.",
            true, List.of(ConfigKey.Scope.Global));

    /**
     * This method will cancel lease on instances running under lease
     * will be primarily used when feature gets disabled
     */
    void cancelLeaseOnExistingInstances();
}
