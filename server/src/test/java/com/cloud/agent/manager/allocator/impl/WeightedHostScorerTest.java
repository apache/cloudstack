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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.host.Host;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class WeightedHostScorerTest {

    private static final HostLoad IDLE = new HostLoad(0.0, 0.0, 10);

    @Mock
    private HostLoadTracker hostLoadTracker;

    @InjectMocks
    private WeightedHostScorer scorer = new WeightedHostScorer();

    private long nextHostId;

    @Before
    public void setUp() {
        nextHostId = 1;
    }

    private Host host(String name) {
        Host host = Mockito.mock(Host.class);
        Mockito.lenient().when(host.getId()).thenReturn(nextHostId++);
        Mockito.lenient().when(host.getName()).thenReturn(name);
        return host;
    }

    private double score(double cpuAllocated, double memAllocated, HostLoad load, long vms, long recentStarts) {
        return scorer.scoreHost(null, cpuAllocated, memAllocated, load, vms, recentStarts);
    }

    @Test
    public void testIdleHostScoresZero() {
        assertEquals(0.0, score(0, 0, IDLE, 0, 0), 1e-9);
    }

    @Test
    public void testMoreAllocationScoresHigher() {
        assertTrue(score(0.5, 0.5, IDLE, 0, 0) > score(0.1, 0.1, IDLE, 0, 0));
    }

    @Test
    public void testBusyHostScoresHigherThanIdleHostWithSameAllocation() {
        HostLoad busy = new HostLoad(0.9, 0.5, 10);
        assertTrue("measured load must separate hosts that look identical by allocation",
                score(0.05, 0.05, busy, 20, 0) > score(0.05, 0.05, IDLE, 20, 0));
    }

    @Test
    public void testUtilisationIgnoredUntilThereAreSamples() {
        // a host with no samples must not be treated as idle, it must rank on allocation alone
        double noSamples = score(0.4, 0.4, HostLoad.UNKNOWN, 10, 0);
        double idleSamples = score(0.4, 0.4, IDLE, 10, 0);
        assertTrue("a host with no load samples should not outrank a measurably idle one",
                noSamples >= idleSamples);
    }

    @Test
    public void testDominantResourcePenalisesLopsidedHost() {
        // same mean across resources, but one host is nearly out of memory
        double balanced = score(0.5, 0.5, IDLE, 0, 0);
        double lopsided = score(0.05, 0.95, IDLE, 0, 0);
        assertTrue("a host nearly out of one resource must not rank as well as an evenly loaded one",
                lopsided > balanced);
    }

    @Test
    public void testDominantResourceUsesUtilisationWhenHigherThanAllocation() {
        HostLoad reclaimedButBusy = new HostLoad(0.95, 0.95, 10);
        assertTrue(score(0.05, 0.05, reclaimedButBusy, 0, 0) > score(0.05, 0.05, IDLE, 0, 0));
    }

    @Test
    public void testVmCountPenalisesHost() {
        assertTrue(score(0.1, 0.1, IDLE, 120, 0) > score(0.1, 0.1, IDLE, 5, 0));
    }

    @Test
    public void testRecentStartsPenaliseHost() {
        assertTrue("VMs that just started are not yet visible in allocation or utilisation",
                score(0.1, 0.1, IDLE, 20, 20) > score(0.1, 0.1, IDLE, 20, 0));
    }

    @Test
    public void testScoreStaysWithinUnitRange() {
        assertTrue(score(1, 1, new HostLoad(1, 1, 10), 1000, 1000) <= 1.0);
        assertTrue(score(0, 0, IDLE, 0, 0) >= 0.0);
    }

    @Test
    public void testBusyHostIsHeldBackByThreshold() {
        Host quiet = host("quiet");
        Host busy = host("busy");
        Mockito.when(hostLoadTracker.getLoad(quiet.getId())).thenReturn(new HostLoad(0.10, 0.10, 10));
        Mockito.when(hostLoadTracker.getLoad(busy.getId())).thenReturn(new HostLoad(0.99, 0.10, 10));

        List<Host> healthy = new ArrayList<>();
        List<Host> tooBusy = new ArrayList<>();
        scorer.partitionByUtilisation(null, new ArrayList<>(List.of(quiet, busy)), healthy, tooBusy);

        assertEquals(1, healthy.size());
        assertSame(quiet, healthy.get(0));
        assertSame("host over the CPU threshold must be held back", busy, tooBusy.get(0));
    }

    @Test
    public void testThresholdIsIgnoredWhenEveryHostIsBusy() {
        Host a = host("a");
        Host b = host("b");
        Mockito.when(hostLoadTracker.getLoad(Mockito.anyLong())).thenReturn(new HostLoad(0.99, 0.99, 10));

        List<Host> healthy = new ArrayList<>();
        List<Host> tooBusy = new ArrayList<>();
        scorer.partitionByUtilisation(null, new ArrayList<>(List.of(a, b)), healthy, tooBusy);

        assertEquals("both hosts are over threshold", 2, tooBusy.size());
        assertTrue(healthy.isEmpty());
    }

    @Test
    public void testSelectionSpreadVariesTheChosenHost() {
        Set<String> chosen = new HashSet<>();
        List<Host> hosts = List.of(host("a"), host("b"), host("c"), host("d"), host("e"));
        for (int i = 0; i < 200; i++) {
            List<Host> ranked = new ArrayList<>(hosts);
            scorer.applySelectionSpread(null, ranked);
            chosen.add(ranked.get(0).getName());
        }
        assertNotEquals("strict ordering sends every concurrent deployment to the same host", 1, chosen.size());
        assertTrue("only the best scoring hosts should be candidates", chosen.size() <= 3);
    }

    @Test
    public void testSelectionSpreadLeavesShortListsAlone() {
        List<Host> ranked = new ArrayList<>(List.of(host("only")));
        scorer.applySelectionSpread(null, ranked);
        assertEquals(1, ranked.size());
    }
}
