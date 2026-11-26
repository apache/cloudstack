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
import java.util.List;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.impl.ConfigDepotImpl;
import org.junit.After;
import org.junit.Before;
import com.cloud.host.HostLoad;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.host.Host;
import com.cloud.host.HostStats;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.server.StatsCollector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class HostLoadTrackerTest {

    private static final long HOST_ID = 1L;
    private static final long HALF_LIFE_MS = 300 * 1000L;

    @Mock
    private ClusterDetailsDao clusterDetailsDao;

    @Mock
    private ClusterDao clusterDao;

    @Mock
    private HostDao hostDao;

    @Mock
    private StatsCollector statsCollector;

    @Mock
    private ConfigDepotImpl configDepot;

    @InjectMocks
    private HostLoadTracker tracker = new HostLoadTracker();

    private long now;

    @Before
    public void setUp() {
        tracker.clear();
        now = 1_000_000L;
        // unstubbed settings read as null, i.e. their defaults
        ConfigKey.init(configDepot);
    }

    @After
    public void tearDown() {
        ConfigKey.init(null);
    }

    private void setting(String name, ConfigKey.Scope scope, Long scopeId, String value) {
        Mockito.lenient().when(configDepot.getConfigStringValue(name, scope, scopeId)).thenReturn(value);
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

    private HostLoad load() {
        return tracker.getLoad(HOST_ID, now);
    }

    @Test
    public void testUnknownHostIsNotUsable() {
        assertFalse(tracker.getLoad(999L, now).isUsable());
    }

    @Test
    public void testFirstSampleIsTakenAsIs() {
        sample(40, 0.6, 0);

        HostLoad load = load();
        assertTrue(load.isUsable());
        assertEquals(0.40, load.getCpuUtilisation(), 1e-6);
        assertEquals(0.60, load.getMemoryUtilisation(), 1e-6);
    }

    @Test
    public void testSingleSpikeDoesNotDominateTheAverage() {
        sample(10, 0.1, 0);
        sample(100, 0.1, 60 * 1000L);

        // one sample a fifth of a half life in should move the average part of the way, not all of it
        double cpu = load().getCpuUtilisation();
        assertTrue("a single spike must not take over the average: " + cpu, cpu < 0.30);
        assertTrue("but it must move it: " + cpu, cpu > 0.10);
    }

    @Test
    public void testSustainedLoadConvergesOnTheNewValue() {
        sample(10, 0.1, 0);
        for (int i = 0; i < 40; i++) {
            sample(90, 0.9, 60 * 1000L);
        }

        HostLoad load = load();
        assertEquals(0.90, load.getCpuUtilisation(), 0.01);
        assertEquals(0.90, load.getMemoryUtilisation(), 0.01);
    }

    @Test
    public void testHalfLifeMovesAverageHalfWay() {
        sample(0, 0, 0);
        sample(100, 1.0, HALF_LIFE_MS);

        assertEquals(0.5, load().getCpuUtilisation(), 0.01);
    }

    @Test
    public void testMissedSamplesDecayByElapsedTimeNotSampleCount() {
        sample(0, 0, 0);
        sample(100, 1.0, 4 * HALF_LIFE_MS);

        // four half lives of catching up in one sample, so almost all the way there
        assertTrue(load().getCpuUtilisation() > 0.9);
    }

    @Test
    public void testNullStatsAreIgnored() {
        tracker.record(HOST_ID, null, now);
        assertFalse(load().isUsable());
    }

    @Test
    public void testHostReportingNoMemoryIsIgnored() {
        HostStats broken = Mockito.mock(HostStats.class);
        Mockito.lenient().when(broken.getTotalMemoryKBs()).thenReturn(0.0);

        tracker.record(HOST_ID, broken, now);

        assertFalse(load().isUsable());
    }

    @Test
    public void testUnchangedReadingIsNotFoldedAgain() {
        // StatsCollector keeps the previous entry when a poll fails, so the same object comes back
        HostStats reading = stats(10, 0.1);
        tracker.record(HOST_ID, reading, now);
        // stay inside the staleness window so this tests folding, not expiry
        for (int i = 0; i < 5; i++) {
            now += 60 * 1000L;
            tracker.record(HOST_ID, reading, now);
        }

        assertEquals("re-reading one measurement must not count as six", 1, load().getSamples());
    }

    @Test
    public void testAHostThatStopsReportingBecomesUnusable() {
        HostStats reading = stats(10, 0.1);
        tracker.record(HOST_ID, reading, now);
        assertTrue(load().isUsable());

        // the agent stops updating; StatsCollector keeps handing back the same stale entry
        for (int i = 0; i < 20; i++) {
            now += 60 * 1000L;
            tracker.record(HOST_ID, reading, now);
        }

        assertFalse("a host that stopped reporting must not keep vouching for itself", load().isUsable());
    }

    @Test
    public void testFreshReadingsKeepAHostUsable() {
        for (int i = 0; i < 20; i++) {
            sample(10 + i, 0.1, 60 * 1000L);
        }
        assertTrue(load().isUsable());
    }

    @Test
    public void testOutOfRangeValuesAreClamped() {
        sample(250, 2.0, 0);

        HostLoad load = load();
        assertEquals(1.0, load.getCpuUtilisation(), 1e-6);
        assertEquals(1.0, load.getMemoryUtilisation(), 1e-6);
    }

    private void clustersWithWeightedDrs(long... clusterIds) {
        List<ClusterDetailsVO> details = new ArrayList<>();
        for (long clusterId : clusterIds) {
            details.add(new ClusterDetailsVO(clusterId, "drs.algorithm", "weighted"));
            Mockito.lenient().when(clusterDao.findById(clusterId)).thenReturn(Mockito.mock(ClusterVO.class));
        }
        Mockito.when(clusterDetailsDao.findDetails("drs.algorithm", "weighted", null)).thenReturn(details);
    }

    @Test
    public void testInUseWhenAClusterUsesWeightedDrsWithTheAllocatorAtItsDefault() {
        clustersWithWeightedDrs(8L);

        assertTrue(tracker.isInUse());
    }

    @Test
    public void testNotInUseWhenOnlyARemovedClusterUsedWeightedDrs() {
        Mockito.when(clusterDetailsDao.findDetails("drs.algorithm", "weighted", null))
                .thenReturn(List.of(new ClusterDetailsVO(9L, "drs.algorithm", "weighted")));
        // findById does not return removed clusters

        assertFalse(tracker.isInUse());
    }

    @Test
    public void testInUseWhenWeightedDrsIsTheGlobalDefaultWithTheAllocatorAtItsDefault() {
        setting("drs.algorithm", ConfigKey.Scope.Global, null, "weighted");

        assertTrue(tracker.isInUse());
        Mockito.verifyNoInteractions(clusterDetailsDao);
    }

    @Test
    public void testNotInUseWhenNeitherTheAllocatorNorAnyClusterReadsTheFigures() {
        clustersWithWeightedDrs();

        assertFalse(tracker.isInUse());
    }

    @Test
    public void testInUseWhenTheAllocatorReadsTheFiguresWithoutLookingAtClusters() {
        setting("vm.allocation.algorithm", ConfigKey.Scope.Global, null, "balancedweighted");

        assertTrue(tracker.isInUse());
        Mockito.verifyNoInteractions(clusterDetailsDao);
    }

    @Test
    public void testHostsAreSampledForWeightedDrsWithTheAllocatorAtItsDefault() {
        clustersWithWeightedDrs(7L);
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getId()).thenReturn(HOST_ID);
        Mockito.when(host.getStatus()).thenReturn(Status.Up);
        Mockito.when(hostDao.listByType(Host.Type.Routing)).thenReturn(List.of(host));
        HostStats reading = stats(40, 0.5);
        Mockito.when(statsCollector.getHostStats(HOST_ID)).thenReturn(reading);

        tracker.sampleAllHosts();

        assertTrue(tracker.getLoad(HOST_ID).isUsable());
    }

    @Test
    public void testSamplesAreDroppedOnceNothingReadsThem() {
        sample(40, 0.5, 0);
        clustersWithWeightedDrs();

        tracker.sampleAllHosts();

        assertFalse(load().isUsable());
        Mockito.verifyNoInteractions(hostDao);
    }
}
