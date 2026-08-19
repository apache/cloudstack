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

package org.apache.cloudstack.vm.bootgroup.readiness;

import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

/**
 * Strategy interface for evaluating one readiness rule type. Implementations are collected by
 * {@code InstanceBootGroupReadinessRuleManagerImpl} via Spring's {@code List<ReadinessChecker>}
 * autowiring and dispatched by {@link #getRuleType()} — an internal implementation detail, not
 * API-facing, so left unprefixed.
 */
public interface ReadinessChecker {

    /**
     * Below this much remaining budget, a checker should not even attempt to dispatch a remote
     * command — there isn't enough time left for a meaningful wait, and dispatching anyway would
     * either use an unhelpfully tiny (or, worse, a zero/negative, which some transports treat as "no
     * override, use the default") wait value.
     */
    long MIN_REMAINING_MS_TO_DISPATCH = 2000L;

    InstanceBootGroupReadinessRule.RuleType getRuleType();

    /**
     * @param remainingMs time budget left for this VM's current attempt; bound any remote dispatch
     *        to it (e.g. via {@code Command.setWait}) and return {@code Status.Error} directly if
     *        it's already too small to be worth dispatching.
     */
    Result check(InstanceBootGroupReadinessRule rule, Map<String, String> details, long vmId, long remainingMs);

    class Result {
        private final InstanceBootGroupReadinessRule.Status status;
        private final String message;

        public Result(InstanceBootGroupReadinessRule.Status status, String message) {
            this.status = status;
            this.message = message;
        }

        public InstanceBootGroupReadinessRule.Status getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }

    default Logger getLogger() {
        return null;
    }

    default Result logAndReturn(InstanceBootGroupReadinessRule rule, Object vmOrId, Result result) {
        Logger log = getLogger();
        if (getLogger() == null) {
            return result;
        }
        Level level = InstanceBootGroupReadinessRule.Status.Ready.equals(result.getStatus())
                ? Level.DEBUG
                : Level.WARN;

        log.log(level, "{} evaluated for {}: status={}, message={}",
                rule, vmOrId, result.getStatus(), result.getMessage());
        return result;
    }

    /**
     * Halves the remaining budget and floors at 1 second for {@code Command.setWait}, which doubles
     * whatever value it's given and treats 0 as "use the global default" rather than "no wait".
     */
    default int computeWaitSeconds(long remainingMs) {
        return (int) Math.max(1, remainingMs / 2000);
    }
}
