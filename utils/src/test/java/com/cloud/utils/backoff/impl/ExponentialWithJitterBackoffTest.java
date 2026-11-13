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

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ExponentialWithJitterBackoffTest {

    private ExponentialWithJitterBackoff backoff;

    @Before
    public void setUp() {
        backoff = new ExponentialWithJitterBackoff();
        Map<String, Object> params = new HashMap<>();
        params.put(ExponentialWithJitterBackoff.MIN_DELAY_MS_CONFIG_KEY, "1");
        params.put(ExponentialWithJitterBackoff.MAX_DELAY_MS_CONFIG_KEY, "8");
        backoff.configure("test", params);
        backoff.reset();
    }

    private int attemptNumber() throws IllegalAccessException {
        return (Integer) FieldUtils.readField(backoff, "attemptNumber", true);
    }

    @Test
    public void testAttemptCyclesUpToMaxAndBackToBase() throws IllegalAccessException {
        // the maximum delay is used for two consecutive waits before the delay decreases back to base
        int[] expected = {1, 2, 3, 3, 2, 1, 0, 1, 2, 3, 3, 2, 1, 0};
        for (int i = 0; i < expected.length; i++) {
            backoff.waitBeforeRetry();
            Assert.assertEquals("attempt number after wait #" + (i + 1), expected[i], attemptNumber());
        }
    }

    @Test
    public void testTimeToWaitStaysWithinBounds() throws IllegalAccessException {
        for (int i = 0; i < 20; i++) {
            long wait = backoff.getTimeToWait();
            long delay = (long) Math.min(Math.pow(2, attemptNumber()), 8);
            Assert.assertTrue("wait " + wait + " below delay " + delay, wait >= delay);
            Assert.assertTrue("wait " + wait + " above delay plus jitter", wait < delay + Math.max(1, delay / 2));
            backoff.waitBeforeRetry();
        }
    }

    @Test
    public void testResetRestartsIncreasingFromBase() throws IllegalAccessException {
        backoff.waitBeforeRetry();
        backoff.waitBeforeRetry();
        backoff.reset();
        Assert.assertEquals(0, attemptNumber());
        backoff.waitBeforeRetry();
        Assert.assertEquals(1, attemptNumber());
    }
}
