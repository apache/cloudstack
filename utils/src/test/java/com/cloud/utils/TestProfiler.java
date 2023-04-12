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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.utils.testcase.Log4jEnabledTestCase;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*",
        "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*" })
@PrepareForTest(Profiler.class)
public class TestProfiler extends Log4jEnabledTestCase {

    private static final long SLEEP_TIME_NANO = 1000000000L;
    private static Profiler pf;

    @Before
    public void setUp() {
        pf = new Profiler();
    }

    @Test
    public void testProfilerInMillis() {
        //Given
        final long sleepTimeMillis = SLEEP_TIME_NANO / 1000000L;

        //When
        pf.start();
        pf.setStartTick(0); // mock start tick
        pf.stop();
        pf.setStopTick(SLEEP_TIME_NANO); // mock stop tick

        //Then
        Assert.assertTrue(pf.getDurationInMillis() == sleepTimeMillis);
    }

    @Test
    public void testProfilerInNano() {
        //Given
        final long sleepTimeNano = SLEEP_TIME_NANO;

        //When
        pf.start();
        pf.setStartTick(0); // mock start tick
        pf.stop();
        pf.setStopTick(SLEEP_TIME_NANO); // mock stop tick

        //Then
        Assert.assertTrue(pf.getDuration() == sleepTimeNano);
    }

    @Test
    public void testProfilerNoStart() {
        //Given
        final long expectedAnswer = -1;

        //When
        pf.stop();
        pf.setStopTick(SLEEP_TIME_NANO); // mock stop tick

        //Then
        Assert.assertTrue(pf.getDurationInMillis() == expectedAnswer);
        Assert.assertFalse(pf.isStarted());
    }

    @Test
    public void testProfilerNoStop() {
        //Given
        final long expectedAnswer = -1;

        //When
        pf.start();

        //Then
        Assert.assertTrue(pf.getDurationInMillis() == expectedAnswer);
        Assert.assertFalse(pf.isStopped());
    }
}
