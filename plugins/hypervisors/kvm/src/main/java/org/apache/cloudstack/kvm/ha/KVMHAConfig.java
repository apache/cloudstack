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

package org.apache.cloudstack.kvm.ha;

import org.apache.cloudstack.framework.config.ConfigKey;

public class KVMHAConfig {

    public static final ConfigKey<Long> KvmHAHealthCheckTimeout = new ConfigKey<>("Advanced", Long.class, "kvm.ha.health.check.timeout", "10",
            "The maximum length of time, in seconds, expected for an health check to complete.", true, ConfigKey.Scope.Cluster);

    public static final ConfigKey<Long> KvmHAActivityCheckTimeout = new ConfigKey<>("Advanced", Long.class, "kvm.ha.activity.check.timeout", "60",
            "The maximum length of time, in seconds, expected for an activity check to complete.", true, ConfigKey.Scope.Cluster);

    public static final ConfigKey<Long> KvmHAActivityCheckInterval = new ConfigKey<>("Advanced", Long.class, "kvm.ha.activity.check.interval", "60",
            "The interval, in seconds, between activity checks.", true, ConfigKey.Scope.Cluster);

    public static final ConfigKey<Long> KvmHAActivityCheckMaxAttempts = new ConfigKey<>("Advanced", Long.class, "kvm.ha.activity.check.max.attempts", "10",
            "The maximum number of activity check attempts to perform before deciding to recover or degrade a resource.", true, ConfigKey.Scope.Cluster);

    public static final ConfigKey<Double> KvmHAActivityCheckFailureThreshold = new ConfigKey<>("Advanced", Double.class, "kvm.ha.activity.check.failure.ratio", "0.7",
            "The activity check failure threshold ratio. This is used with the activity check maximum attempts for deciding to recover or degrade a resource. For most environments, please keep this value above 0.5.",
            true, ConfigKey.Scope.Cluster);

    public static final ConfigKey<Long> KvmHADegradedMaxPeriod = new ConfigKey<>("Advanced", Long.class, "kvm.ha.degraded.max.period", "300",
            "The maximum length of time, in seconds, a resource can be in degraded state where only health checks are performed.", true, ConfigKey.Scope.Cluster);

    public static final ConfigKey<Long> KvmHARecoverTimeout = new ConfigKey<>("Advanced", Long.class, "kvm.ha.recover.timeout", "60",
            "The maximum length of time, in seconds, expected for a recovery operation to complete.", true, ConfigKey.Scope.Cluster);

    public static final ConfigKey<Long> KvmHARecoverWaitPeriod = new ConfigKey<>("Advanced", Long.class, "kvm.ha.recover.wait.period", "600",
            "The maximum length of time, in seconds, to wait for a resource to recover.", true, ConfigKey.Scope.Cluster);

    public static final ConfigKey<Long> KvmHARecoverAttemptThreshold = new ConfigKey<>("Advanced", Long.class, "kvm.ha.recover.failure.threshold", "1",
            "The maximum recovery attempts to be made for a resource, after which the resource is fenced. The recovery counter resets when a health check passes for a resource.",
            true, ConfigKey.Scope.Cluster);

    public static final ConfigKey<Long> KvmHAFenceTimeout = new ConfigKey<>("Advanced", Long.class, "kvm.ha.fence.timeout", "60",
            "The maximum length of time, in seconds, expected for a fence operation to complete.", true, ConfigKey.Scope.Cluster);

}
