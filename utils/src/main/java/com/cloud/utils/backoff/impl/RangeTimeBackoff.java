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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.log4j.Logger;

import com.cloud.utils.NumbersUtil;
import com.cloud.utils.backoff.BackoffAlgorithm;
import com.cloud.utils.component.AdapterBase;

/**
 * An implementation of BackoffAlgorithm that waits for some random seconds
 * within a given range.
 * After the time the client can try to perform the operation again.
 *
 **/
public class RangeTimeBackoff extends AdapterBase implements BackoffAlgorithm {
    protected static final int DEFAULT_MIN_TIME = 5;
    private int minTime = DEFAULT_MIN_TIME;
    private int maxTime = 3 * DEFAULT_MIN_TIME;
    private final Map<String, Thread> asleep = new ConcurrentHashMap<>();
    private static final Logger LOG = Logger.getLogger(RangeTimeBackoff.class.getName());

    @Override
    public void waitBeforeRetry() {
        long time = minTime * 1000L;
        Thread current = Thread.currentThread();
        try {
            asleep.put(current.getName(), current);
            time = ThreadLocalRandom.current().nextInt(minTime, maxTime) * 1000L;
            LOG.debug("Waiting " + current.getName() + " for " + time);
            Thread.sleep(time);
        } catch (InterruptedException e) {
            // JMX or other threads may interrupt this thread, but let's log it
            // anyway, no exception to log as this is not an error
            LOG.info("Thread " + current.getName() + " interrupted while waiting for retry");
        } finally {
            asleep.remove(current.getName());
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) {
        minTime = NumbersUtil.parseInt((String)params.get("minSeconds"), DEFAULT_MIN_TIME);
        maxTime = NumbersUtil.parseInt((String)params.get("maxSeconds"), minTime * 3);
        return true;
    }
}
