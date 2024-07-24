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

package org.apache.cloudstack.ha;

import com.cloud.ha.Investigator;
import com.cloud.host.Host;
import com.cloud.host.Status;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.ha.provider.HAProvider;

public interface HAManager extends HAConfigManager {

    ConfigKey<Integer> MaxConcurrentHealthCheckOperations = new ConfigKey<>("Advanced", Integer.class,
            "ha.max.concurrent.health.check.operations",
            "50",
            "The number of concurrent health check operations per management server. This setting determines the size of the thread pool consuming the HEALTH CHECK queue.", true);

    ConfigKey<Integer> MaxPendingHealthCheckOperations = new ConfigKey<>("Advanced", Integer.class,
            "ha.max.pending.health.check.operations",
            "5000",
            "The number of pending health check operations per management server. This setting determines the size of the HEALTH CHECK queue.", true);

    ConfigKey<Integer> MaxConcurrentActivityCheckOperations = new ConfigKey<>("Advanced", Integer.class,
            "ha.max.concurrent.activity.check.operations",
            "25",
            "The number of concurrent activity check operations per management server. This setting determines the size of the thread pool consuming the ACTIVITY CHECK queue.",
            true);

    ConfigKey<Integer> MaxPendingActivityCheckOperations = new ConfigKey<>("Advanced", Integer.class,
            "ha.max.pending.activity.check.operations",
            "2500",
            "The number of pending activity check operations per management server. This setting determines the size of the size of the ACTIVITY CHECK queue.", true);

    ConfigKey<Integer> MaxConcurrentRecoveryOperations = new ConfigKey<>("Advanced", Integer.class,
            "ha.max.concurrent.recovery.operations",
            "25",
            "The number of concurrent recovery operations per management server.", true);

    ConfigKey<Integer> MaxPendingRecoveryOperations = new ConfigKey<>("Advanced", Integer.class,
            "ha.max.pending.recovery.operations",
            "2500",
            "The number of pending recovery operations per management server. This setting determines the size of the size of the RECOVERY queue.", true);

    ConfigKey<Integer> MaxConcurrentFenceOperations = new ConfigKey<>("Advanced", Integer.class,
            "ha.max.concurrent.fence.operations",
            "25",
            "The number of concurrent fence operations per management server.", true);

    ConfigKey<Integer> MaxPendingFenceOperations = new ConfigKey<>("Advanced", Integer.class,
            "ha.max.pending.fence.operations",
            "2500",
            "The number of pending fence operations per management server. This setting determines the size of the size of the FENCE queue.", true);

    boolean transitionHAState(final HAConfig.Event event, final HAConfig haConfig);
    HAProvider getHAProvider(final String name);
    HAResourceCounter getHACounter(final Long resourceId, final HAResource.ResourceType resourceType);
    void purgeHACounter(final Long resourceId, final HAResource.ResourceType resourceType);

    boolean isHAEligible(final HAResource resource);
    Boolean isVMAliveOnHost(final Host host) throws Investigator.UnknownVM;
    Status getHostStatus(final Host host);
}
