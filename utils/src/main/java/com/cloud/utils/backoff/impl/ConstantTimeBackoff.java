//
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
//

package com.cloud.utils.backoff.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.cloud.utils.NumbersUtil;
import com.cloud.utils.backoff.BackoffAlgorithm;
import com.cloud.utils.backoff.BackoffFactory;
import com.cloud.utils.component.AdapterBase;

/**
 * An implementation of BackoffAlgorithm that waits for some seconds.
 * After the time the client can try to perform the operation again.
 *
 * @config
 * {@table
 *    || Param Name | Description | Values | Default ||
 *    || seconds    | seconds to sleep | integer | 5 ||
 *  }
 **/
public class ConstantTimeBackoff extends AdapterBase implements BackoffAlgorithm, ConstantTimeBackoffMBean {
    /**
     * Property name for the delay between retries to be used either by {@code agent.properties} file or by configuration key.
     */
    public static final String DELAY_SEC_CONFIG_KEY = "backoff.seconds";

    /**
     * Default value for the delay between retries for the property {@link ConstantTimeBackoff#DELAY_SEC_CONFIG_KEY}.
     */
    public static final int DELAY_SEC_DEFAULT = 5;

    private long _time;
    private final Map<String, Thread> _asleep = new ConcurrentHashMap<String, Thread>();

    @Override
    public void waitBeforeRetry() {
        Thread current = Thread.currentThread();
        try {
            _asleep.put(current.getName(), current);
            Thread.sleep(_time);
        } catch (InterruptedException e) {
            // JMX or other threads may interrupt this thread, but let's log it
            // anyway, no exception to log as this is not an error
            logger.info("Thread " + current.getName() + " interrupted while waiting for retry");
        } finally {
            _asleep.remove(current.getName());
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public Map<String, String> getConfiguration() {
        Map<String, String> configuration = new HashMap<>();
        configuration.put(BackoffFactory.BACKOFF_IMPLEMENTATION_KEY, getClass().getName());
        configuration.put(DELAY_SEC_CONFIG_KEY, String.valueOf(_time / 1000));
        return configuration;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) {
        _time = NumbersUtil.parseLong((String) params.get(DELAY_SEC_CONFIG_KEY), DELAY_SEC_DEFAULT) * 1000;
        return true;
    }

    @Override
    public Collection<String> getWaiters() {
        return _asleep.keySet();
    }

    @Override
    public boolean wakeup(String threadName) {
        Thread th = _asleep.get(threadName);
        if (th != null) {
            th.interrupt();
            return true;
        }

        return false;
    }

    @Override
    public long getTimeToWait() {
        return _time;
    }

    @Override
    public void setTimeToWait(long seconds) {
        _time = seconds * 1000;
    }
}
