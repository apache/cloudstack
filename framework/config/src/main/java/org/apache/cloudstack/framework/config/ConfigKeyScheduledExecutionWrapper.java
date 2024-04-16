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
package org.apache.cloudstack.framework.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * Uses a ScheduledExecutorService and config key to execute a runnable,
 * dynamically rescheduling based on the long value of the config key.
 * Timing is similar to ScheduledExecutorService.scheduleAtFixedRate(),
 * but we look up the next runtime dynamically via the config key.
 * <p>
 * If config key is zero, this disables the execution. We skip execution
 * and check once a minute in order to re-start execution if re-enabled.
 */
public class ConfigKeyScheduledExecutionWrapper implements Runnable {
    protected Logger logger = LogManager.getLogger(getClass());
    private final ScheduledExecutorService executorService;
    private final Runnable command;
    private final ConfigKey<?> configKey;
    private final TimeUnit unit;
    private long enableIntervalSeconds = 60;

    private void validateArgs(ScheduledExecutorService executorService, Runnable command, ConfigKey<?> configKey) {
        if (executorService == null) {
            throw new IllegalArgumentException("ExecutorService cannot be null");
        }
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (configKey == null) {
            throw new IllegalArgumentException("ConfigKey cannot be null");
        }
        if (!(configKey.value() instanceof Long || configKey.value() instanceof Integer)) {
            throw new IllegalArgumentException("ConfigKey value must be a Long or Integer");
        }
    }

    public ConfigKeyScheduledExecutionWrapper(ScheduledExecutorService executorService, Runnable command,
            ConfigKey<?> configKey, TimeUnit unit) {
        validateArgs(executorService, command, configKey);
        this.executorService = executorService;
        this.command = command;
        this.configKey = configKey;
        this.unit = unit;
    }

    protected ConfigKeyScheduledExecutionWrapper(ScheduledExecutorService executorService, Runnable command,
            ConfigKey<?> configKey, int enableIntervalSeconds, TimeUnit unit) {
        validateArgs(executorService, command, configKey);
        this.executorService = executorService;
        this.command = command;
        this.configKey = configKey;
        this.unit = unit;
        this.enableIntervalSeconds = enableIntervalSeconds;
    }

    public ScheduledFuture<?> start() {
        long duration = getConfigValue();
        duration = duration < 0 ? 0 : duration;
        return this.executorService.schedule(this, duration, this.unit);
    }

    long getConfigValue() {
        if (this.configKey.value() instanceof Long) {
            return (Long) this.configKey.value();
        } else if (this.configKey.value() instanceof Integer) {
            return (Integer) this.configKey.value();
        } else {
            throw new IllegalArgumentException("ConfigKey value must be a Long or Integer");
        }
    }

    @Override
    public void run() {
        if (getConfigValue() <= 0) {
            executorService.schedule(this, enableIntervalSeconds, TimeUnit.SECONDS);
            return;
        }

        long startTime = System.nanoTime();
        try {
            command.run();
        } catch (Throwable t) {
            logger.warn(String.format("Last run of %s encountered an error", this.command.getClass()), t);
        } finally {
            long elapsed = System.nanoTime() - startTime;
            long delay = this.unit.toNanos(getConfigValue()) - elapsed;
            delay = delay > 0 ? delay : 0;
            executorService.schedule(this, delay, NANOSECONDS);
        }
    }
}
