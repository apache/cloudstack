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

package com.cloud.utils;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.cloud.utils.testcase.Log4jEnabledTestCase;

public class TestProfiler extends Log4jEnabledTestCase {
    protected final static Logger s_logger = Logger.getLogger(TestProfiler.class);

    private static final long ONE_SECOND = 1000l;
    private static final long MILLIS_FACTOR = 1000l;
    private static final int MARGIN = 100;
    private static final double EXPONENT = 3d;

    @Test
    public void testProfilerInMillis() {
        s_logger.info("testProfiler() started");

        final Profiler pf = new Profiler();
        pf.start();
        try {
            Thread.sleep(ONE_SECOND);
        } catch (final InterruptedException e) {
        }
        pf.stop();

        final long durationInMillis = pf.getDurationInMillis();
        s_logger.info("Duration in Millis: " + durationInMillis);

        // An error margin in order to cover the time taken by the star/stop calls.
        // 100 milliseconds margin seems too much, but it will avoid assertion error
        // and also fail in case a duration in nanoseconds is used instead.
        Assert.assertTrue(durationInMillis >= MILLIS_FACTOR  &&  durationInMillis <= MILLIS_FACTOR + MARGIN);

        s_logger.info("testProfiler() stopped");
    }

    @Test
    public void testProfilerInNano() {
        final Profiler pf = new Profiler();
        pf.start();
        try {
            Thread.sleep(ONE_SECOND);
        } catch (final InterruptedException e) {
        }
        pf.stop();

        final long duration = pf.getDuration();
        s_logger.info("Duration in Nano: " + duration);
        Assert.assertTrue(duration >= Math.pow(MILLIS_FACTOR, EXPONENT));
    }

    @Test
    public void testProfilerNoStart() {
        final Profiler pf = new Profiler();
        try {
            Thread.sleep(20);
        } catch (final InterruptedException e) {
        }
        pf.stop();

        Assert.assertTrue(pf.getDurationInMillis() == -1);
        Assert.assertFalse(pf.isStarted());
    }

    @Test
    public void testProfilerNoStop() {
        final Profiler pf = new Profiler();
        pf.start();
        try {
            Thread.sleep(20);
        } catch (final InterruptedException e) {
        }

        Assert.assertTrue(pf.getDurationInMillis() == -1);
        Assert.assertFalse(pf.isStopped());
    }

    @Test
    public void testResolution() {
        long nanoTime1 = 0l;
        long nanoTime2 = 0l;
        nanoTime1 = System.nanoTime();
        nanoTime2 = System.nanoTime();

        // Using sysout here because is faster than the logger and we don't want to
        // waste time.
        System.out.println("Nano time 1: " + nanoTime1);
        System.out.println("Nano time 2: " + nanoTime2);

        // We are measuring the elapsed time in 2 consecutive calls of System.nanoTime()
        // That's the same as 0.002 milliseconds or 2000 nanoseconds.
        Assert.assertTrue("Expected exactly 2 but it took more than 3 microseconds between 2 consecutive calls to System.nanoTime().", nanoTime2 - nanoTime1 <= 3000);
    }
}