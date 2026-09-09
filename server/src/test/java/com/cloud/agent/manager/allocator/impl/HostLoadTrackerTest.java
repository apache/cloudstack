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
package com.cloud.agent.manager.allocator.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.host.HostStats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class HostLoadTrackerTest {

    private static final long HOST_ID = 1L;
    private static final long HALF_LIFE_MS = 300 * 1000L;

    @InjectMocks
    private HostLoadTracker tracker = new HostLoadTracker();

    private long now;

    @Before
    public void setUp() {
        tracker.clear();
        now = 1_000_000L;
    }

    private HostStats stats(double cpuPercent, double usedMemoryFraction) {
        HostStats stats = Mockito.mock(HostStats.class);
        Mockito.lenient().when(stats.getCpuUtilization()).thenReturn(cpuPercent);
        Mockito.lenient().when(stats.getTotalMemoryKBs()).thenReturn(1000.0);
        Mockito.lenient().when(stats.getFreeMemoryKBs()).thenReturn(1000.0 * (1 - usedMemoryFraction));
        return stats;
    }

    private void sample(double cpuPercent, double usedMemoryFraction, long advanceMs) {
        now += advanceMs;
        tracker.record(HOST_ID, stats(cpuPercent, usedMemoryFraction), now);
    }

    @Test
    public void testUnknownHostIsNotUsable() {
        assertFalse(tracker.getLoad(999L).isUsable());
    }

    @Test
    public void testFirstSampleIsTakenAsIs() {
        sample(40, 0.6, 0);

        HostLoad load = tracker.getLoad(HOST_ID);
        assertTrue(load.isUsable());
        assertEquals(0.40, load.getCpuUtilisation(), 1e-6);
        assertEquals(0.60, load.getMemoryUtilisation(), 1e-6);
    }

    @Test
    public void testSingleSpikeDoesNotDominateTheAverage() {
        sample(10, 0.1, 0);
        sample(100, 0.1, 60 * 1000L);

        // one sample a fifth of a half life in should move the average part of the way, not all of it
        double cpu = tracker.getLoad(HOST_ID).getCpuUtilisation();
        assertTrue("a single spike must not take over the average: " + cpu, cpu < 0.30);
        assertTrue("but it must move it: " + cpu, cpu > 0.10);
    }

    @Test
    public void testSustainedLoadConvergesOnTheNewValue() {
        sample(10, 0.1, 0);
        for (int i = 0; i < 40; i++) {
            sample(90, 0.9, 60 * 1000L);
        }

        HostLoad load = tracker.getLoad(HOST_ID);
        assertEquals(0.90, load.getCpuUtilisation(), 0.01);
        assertEquals(0.90, load.getMemoryUtilisation(), 0.01);
    }

    @Test
    public void testHalfLifeMovesAverageHalfWay() {
        sample(0, 0, 0);
        sample(100, 1.0, HALF_LIFE_MS);

        assertEquals(0.5, tracker.getLoad(HOST_ID).getCpuUtilisation(), 0.01);
    }

    @Test
    public void testMissedSamplesDecayByElapsedTimeNotSampleCount() {
        sample(0, 0, 0);
        sample(100, 1.0, 4 * HALF_LIFE_MS);

        // four half lives of catching up in one sample, so almost all the way there
        assertTrue(tracker.getLoad(HOST_ID).getCpuUtilisation() > 0.9);
    }

    @Test
    public void testNullStatsAreIgnored() {
        tracker.record(HOST_ID, null, now);
        assertFalse(tracker.getLoad(HOST_ID).isUsable());
    }

    @Test
    public void testHostReportingNoMemoryIsIgnored() {
        HostStats broken = Mockito.mock(HostStats.class);
        Mockito.lenient().when(broken.getTotalMemoryKBs()).thenReturn(0.0);

        tracker.record(HOST_ID, broken, now);

        assertFalse(tracker.getLoad(HOST_ID).isUsable());
    }

    @Test
    public void testOutOfRangeValuesAreClamped() {
        sample(250, 2.0, 0);

        HostLoad load = tracker.getLoad(HOST_ID);
        assertEquals(1.0, load.getCpuUtilisation(), 1e-6);
        assertEquals(1.0, load.getMemoryUtilisation(), 1e-6);
    }
}
