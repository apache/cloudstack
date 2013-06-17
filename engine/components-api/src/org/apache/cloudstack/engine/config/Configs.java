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
package org.apache.cloudstack.engine.config;

import org.apache.cloudstack.config.ConfigKey;
import org.apache.cloudstack.engine.service.api.OrchestrationService;

public interface Configs {
    
    public static final ConfigKey<Integer> StartRetry = new ConfigKey<Integer>(
            Integer.class, "start.retry", "Advanced", OrchestrationService.class, "10", "Number of times to retry create and start commands", null);
    public static final ConfigKey<Long> VmOpWaitInterval = new ConfigKey<Long>(
            Long.class, "vm.op.wait.interval", "Advanced", OrchestrationService.class, "120", "Time (in seconds) to wait before checking if a previous operation has succeeded",
            null);
    public static final ConfigKey<Integer> VmOpLockStateRetry = new ConfigKey<Integer>(
            Integer.class, "vm.op.lock.state.retry", "Advanced", OrchestrationService.class, "5", "Times to retry locking the state of a VM for operations", "-1 means try forever");
    public static final ConfigKey<Long> VmOpCleanupInterval = new ConfigKey<Long>(
            Long.class, "vm.op.cleanup.interval", "Advanced", OrchestrationService.class, "86400", "Interval to run the thread that cleans up the vm operations (in seconds)",
            "Seconds");
    public static final ConfigKey<Long> VmOpCleanupWait = new ConfigKey<Long>(
            Long.class, "vm.op.cleanup.wait", "Advanced", OrchestrationService.class, "3600", "Time (in seconds) to wait before cleanuping up any vm work items", "Seconds");
    public static final ConfigKey<Integer> VmOpCancelInterval = new ConfigKey<Integer>(
            Integer.class, "vm.op.cancel.interval", "Advanced", OrchestrationService.class, "3600", "Time (in seconds) to wait before cancelling a operation", "Seconds");

    public static final ConfigKey<Integer> Wait = new ConfigKey<Integer>(
            Integer.class, "wait", "Advanced", OrchestrationService.class, "1800", "Time in seconds to wait for control commands to return", null);
    public static final ConfigKey<Boolean> VmDestroyForcestop = new ConfigKey<Boolean>(
            Boolean.class, "vm.destroy.forcestop", "Advanced", OrchestrationService.class, "false", "On destroy, force-stop takes this value ", null);

    public static final ConfigKey<Long> PingInterval = new ConfigKey<Long>(
            Long.class, "ping.interval", "Advanced", OrchestrationService.class, "60", "Ping interval in seconds", null);
}
