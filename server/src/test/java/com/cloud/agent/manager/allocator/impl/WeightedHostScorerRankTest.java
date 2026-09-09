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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.host.Host;
import com.cloud.utils.Pair;
import com.cloud.vm.dao.VMInstanceDao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Exercises rank() as the allocator calls it, rather than the scoring function alone, so the
 * capacity denominator, the utilisation thresholds and the random spread are all covered.
 */
@RunWith(MockitoJUnitRunner.class)
public class WeightedHostScorerRankTest {

    private static final long ZONE = 1L;
    private static final long CLUSTER = 7L;
    private static final long CORES = 192L * 2400L;
    private static final long MEMORY = 1_132_000L * 1024L * 1024L;
    private static final int CPU_OVERCOMMIT = 10;
    private static final int MEMORY_OVERCOMMIT = 4;

    @Mock
    private CapacityDao capacityDao;

    @Mock
    private VMInstanceDao vmInstanceDao;

    @Mock
    private ClusterDetailsDao clusterDetailsDao;

    @Mock
    private HostLoadTracker hostLoadTracker;

    @InjectMocks
    private WeightedHostScorer scorer = new WeightedHostScorer();

    private final List<CapacityVO> capacities = new ArrayList<>();
    private final Map<Long, Pair<Long, Long>> vmCounts = new HashMap<>();
    private final Map<Long, Host> hosts = new HashMap<>();

    @Before
    public void setUp() {
        scorer.random = new Random(1L);
        Mockito.lenient().when(clusterDetailsDao.findDetail(Mockito.eq(CLUSTER), Mockito.contains("cpu")))
                .thenReturn(new ClusterDetailsVO(CLUSTER, "cpuOvercommitRatio", String.valueOf(CPU_OVERCOMMIT)));
        Mockito.lenient().when(clusterDetailsDao.findDetail(Mockito.eq(CLUSTER), Mockito.contains("memory")))
                .thenReturn(new ClusterDetailsVO(CLUSTER, "memoryOvercommitRatio", String.valueOf(MEMORY_OVERCOMMIT)));
        Mockito.lenient().when(capacityDao.listHostCapacityByCapacityTypes(Mockito.eq(ZONE), Mockito.eq(CLUSTER), Mockito.any()))
                .thenReturn(capacities);
        Mockito.lenient().when(vmInstanceDao.countVmsByHost(Mockito.eq(ZONE), Mockito.any(), Mockito.eq(CLUSTER), Mockito.any()))
                .thenReturn(vmCounts);
    }

    private CapacityVO capacity(long hostId, short type, long used, long total) {
        CapacityVO capacity = new CapacityVO(hostId, ZONE, 1L, CLUSTER, used, total, type);
        capacity.setReservedCapacity(0L);
        return capacity;
    }

    /** Registers a host with a share of its allocatable CPU and memory already committed. */
    private Host host(long id, String name, double cpuAllocatedFraction, double memoryAllocatedFraction,
            HostLoad load, long vms) {
        Host host = Mockito.mock(Host.class);
        Mockito.lenient().when(host.getId()).thenReturn(id);
        Mockito.lenient().when(host.getName()).thenReturn(name);
        capacities.add(capacity(id, Capacity.CAPACITY_TYPE_CPU,
                (long) (CORES * CPU_OVERCOMMIT * cpuAllocatedFraction), CORES));
        capacities.add(capacity(id, Capacity.CAPACITY_TYPE_MEMORY,
                (long) (MEMORY * MEMORY_OVERCOMMIT * memoryAllocatedFraction), MEMORY));
        vmCounts.put(id, new Pair<>(vms, 0L));
        Mockito.lenient().when(hostLoadTracker.getLoad(id)).thenReturn(load);
        hosts.put(id, host);
        return host;
    }

    private List<String> rankedNames(List<? extends Host> input) {
        return scorer.rank(ZONE, 1L, CLUSTER, input).stream().map(Host::getName).collect(Collectors.toList());
    }

    @Test
    public void testAllocationIsMeasuredAgainstTheOvercommittedTotal() {
        // both hosts are well past their physical CPU, which is normal at a factor of 10.
        // if the denominator ignored the factor both would clamp to 1.0 and rank equal.
        Host light = host(1L, "light", 0.20, 0.20, new HostLoad(0.1, 0.1, 5), 20);
        Host heavy = host(2L, "heavy", 0.80, 0.20, new HostLoad(0.1, 0.1, 5), 20);

        Map<Long, Double> scores = scorer.score(ZONE, 1L, CLUSTER, Arrays.asList(light, heavy));

        assertTrue("hosts past their physical size must still be distinguishable",
                scores.get(heavy.getId()) > scores.get(light.getId()));
    }

    @Test
    public void testBusyHostRanksBehindQuietOneAtEqualAllocation() {
        Host quiet = host(1L, "quiet", 0.30, 0.30, new HostLoad(0.05, 0.05, 10), 30);
        Host busy = host(2L, "busy", 0.30, 0.30, new HostLoad(0.70, 0.30, 10), 30);

        assertEquals("quiet", rankedNames(Arrays.asList(busy, quiet)).get(0));
    }

    @Test
    public void testHostOverThresholdIsNotChosenWhileAHealthyOneExists() {
        // one healthy host and five over threshold: the spread must not shuffle a busy host in front
        List<Host> input = new ArrayList<>();
        input.add(host(1L, "healthy", 0.30, 0.30, new HostLoad(0.50, 0.50, 10), 30));
        for (long id = 2; id <= 6; id++) {
            input.add(host(id, "busy" + id, 0.30, 0.30, new HostLoad(0.95, 0.50, 10), 30));
        }

        for (int attempt = 0; attempt < 50; attempt++) {
            assertEquals("the only healthy host must always lead", "healthy", rankedNames(input).get(0));
        }
    }

    @Test
    public void testUnmeasuredHostRanksBehindEveryMeasuredHost() {
        // a host whose stats have stopped must not look idle and collect the deployments
        Host measured = host(1L, "measured", 0.60, 0.60, new HostLoad(0.40, 0.40, 10), 60);
        Host unmeasured = host(2L, "unmeasured", 0.05, 0.05, HostLoad.UNKNOWN, 2);

        List<String> ranked = rankedNames(Arrays.asList(unmeasured, measured));

        assertEquals("measured", ranked.get(0));
        assertEquals("unmeasured", ranked.get(1));
    }

    @Test
    public void testRankingFallsBackToAllocationWhenNothingIsMeasured() {
        Host light = host(1L, "light", 0.10, 0.10, HostLoad.UNKNOWN, 5);
        Host heavy = host(2L, "heavy", 0.90, 0.90, HostLoad.UNKNOWN, 90);

        assertEquals("light", rankedNames(Arrays.asList(heavy, light)).get(0));
    }

    @Test
    public void testEveryHostIsStillOfferedWhenTheWholeClusterIsBusy() {
        List<Host> input = Arrays.asList(
                host(1L, "a", 0.30, 0.30, new HostLoad(0.95, 0.50, 10), 30),
                host(2L, "b", 0.40, 0.30, new HostLoad(0.97, 0.50, 10), 40));

        List<String> ranked = rankedNames(input);

        assertEquals("deployment must remain possible", 2, ranked.size());
    }

    @Test
    public void testSpreadVariesTheLeadAmongHealthyHosts() {
        List<Host> input = Arrays.asList(
                host(1L, "a", 0.30, 0.30, new HostLoad(0.10, 0.10, 10), 30),
                host(2L, "b", 0.31, 0.30, new HostLoad(0.10, 0.10, 10), 30),
                host(3L, "c", 0.32, 0.30, new HostLoad(0.10, 0.10, 10), 30),
                host(4L, "d", 0.90, 0.30, new HostLoad(0.10, 0.10, 10), 90));

        Set<String> leaders = new HashSet<>();
        for (int attempt = 0; attempt < 200; attempt++) {
            leaders.add(rankedNames(input).get(0));
        }

        assertTrue("concurrent deployments must not all pick one host", leaders.size() > 1);
        assertFalse("the clearly worst host must never lead", leaders.contains("d"));
    }

    @Test
    public void testHostMissingACapacityRowIsNotRankedFirst() {
        Host complete = host(1L, "complete", 0.60, 0.60, new HostLoad(0.40, 0.40, 10), 60);
        Host partial = Mockito.mock(Host.class);
        Mockito.lenient().when(partial.getId()).thenReturn(2L);
        Mockito.lenient().when(partial.getName()).thenReturn("partial");
        // only a CPU row: memory must not be treated as untouched
        capacities.add(capacity(2L, Capacity.CAPACITY_TYPE_CPU, 0L, CORES));

        List<String> ranked = rankedNames(Arrays.asList(partial, complete));

        assertEquals("complete", ranked.get(0));
        assertEquals("partial", ranked.get(1));
    }
}
