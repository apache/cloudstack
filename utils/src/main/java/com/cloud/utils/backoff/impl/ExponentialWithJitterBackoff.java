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
package com.cloud.utils.backoff.impl;

import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.backoff.BackoffAlgorithm;
import com.cloud.utils.backoff.BackoffFactory;
import com.cloud.utils.component.AdapterBase;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Exponential backoff with up/down cycling.
 * Delay grows exponentially until a maximum, then decreases back to base, then repeats.
 *
 * @author mprokopchuk
 */
public class ExponentialWithJitterBackoff extends AdapterBase implements BackoffAlgorithm,
        ExponentialWithJitterBackoffMBean {

    /**
     * Property name for the minimal delay to be used either by {@code agent.properties} file or by configuration key.
     */
    public static final String MIN_DELAY_MS_CONFIG_KEY = "backoff.min_delay_ms";

    /**
     * Property name for the maximal delay to be used either by {@code agent.properties} file or by configuration key.
     */
    public static final String MAX_DELAY_MS_CONFIG_KEY = "backoff.max_delay_ms";

    /**
     * Default value for minimal delay for the property {@link ExponentialWithJitterBackoff#MIN_DELAY_MS_DEFAULT}.
     */
    public static final int MIN_DELAY_MS_DEFAULT = 5_000;

    /**
     * Default value for maximal delay for the property {@link ExponentialWithJitterBackoff#MAX_DELAY_MS_DEFAULT}.
     */
    public static final int MAX_DELAY_MS_DEFAULT = 15_000;

    private final Map<String, Thread> asleep = new ConcurrentHashMap<>();
    private final Random random = new SecureRandom();

    private int minDelayMs;
    private int maxDelayMs;
    private int maxAttempts;
    private int attemptNumber;
    private boolean increasing;

    @Override
    public void waitBeforeRetry() {
        boolean interrupted = false;
        long waitMs = getTimeToWait();
        Thread current = Thread.currentThread();
        try {
            asleep.put(current.getName(), current);
            logger.debug(String.format("Going to sleep for %s", DateUtil.formatMillis(waitMs)));
            Thread.sleep(waitMs);
            logger.debug(String.format("Sleep done for %s", DateUtil.formatMillis(waitMs)));
        } catch (InterruptedException e) {
            logger.info(String.format("Thread %s interrupted while waiting for retry", current.getName()), e);
        } finally {
            asleep.remove(current.getName());
            calculateNextAttempt();
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Calculates next attempt and direction.
     */
    private void calculateNextAttempt() {
        if (increasing) {
            int nextAttemptNumber = attemptNumber + 1;
            increasing = getNextDelay() <= maxDelayMs && nextAttemptNumber <= maxAttempts;
            if (increasing) {
                attemptNumber = nextAttemptNumber;
            }
        } else {
            int nextAttemptNumber = Math.max(attemptNumber - 1, 0);
            increasing = nextAttemptNumber == 0;
            if (!increasing) {
                attemptNumber = nextAttemptNumber;
            }
        }
    }

    @Override
    public void reset() {
        attemptNumber = 0;
    }

    @Override
    public Map<String, String> getConfiguration() {
        Map<String, String> configuration = new HashMap<>();
        configuration.put(MIN_DELAY_MS_CONFIG_KEY, String.valueOf(minDelayMs));
        configuration.put(MAX_DELAY_MS_CONFIG_KEY, String.valueOf(maxDelayMs));
        configuration.put(BackoffFactory.BACKOFF_IMPLEMENTATION_KEY, this.getClass().getName());
        return configuration;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) {
        minDelayMs = NumbersUtil.parseInt((String) params.get(MIN_DELAY_MS_CONFIG_KEY), MIN_DELAY_MS_DEFAULT);
        maxDelayMs = NumbersUtil.parseInt((String) params.get(MAX_DELAY_MS_CONFIG_KEY), MAX_DELAY_MS_DEFAULT);
        maxAttempts = (int) Math.round(Math.log((double) maxDelayMs / minDelayMs) / Math.log(2));

        attemptNumber = random.nextInt(maxAttempts + 1);
        increasing = random.nextBoolean();
        // do nothing
        return true;
    }

    @Override
    public Collection<String> getWaiters() {
        return asleep.keySet();
    }

    @Override
    public boolean wakeup(String threadName) {
        Thread th = asleep.get(threadName);
        if (th != null) {
            th.interrupt();
            return true;
        }
        return false;
    }

    private long getNextDelay() {
        return (long) Math.min(minDelayMs * Math.pow(2, attemptNumber), maxDelayMs);
    }

    @Override
    public long getTimeToWait() {
        long delay = getNextDelay();
        int jitter = random.nextInt((int) delay / 2);
        return delay + jitter;
    }

    @Override
    public void setTimeToWait(long seconds) {
        // ignore
    }
}
