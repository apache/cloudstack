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
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.host.Host;
import com.cloud.utils.Ternary;
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
    private final Map<Long, Ternary<Long, Long, Long>> vmCounts = new HashMap<>();
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
        return host(id, name, cpuAllocatedFraction, memoryAllocatedFraction, load, vms, 0L);
    }

    /** As above, with a number of VMs left sitting in Starting on the host. */
    private Host host(long id, String name, double cpuAllocatedFraction, double memoryAllocatedFraction,
            HostLoad load, long vms, long startingVms) {
        Host host = Mockito.mock(Host.class);
        Mockito.lenient().when(host.getId()).thenReturn(id);
        Mockito.lenient().when(host.getName()).thenReturn(name);
        capacities.add(capacity(id, Capacity.CAPACITY_TYPE_CPU,
                (long) (CORES * CPU_OVERCOMMIT * cpuAllocatedFraction), CORES));
        capacities.add(capacity(id, Capacity.CAPACITY_TYPE_MEMORY,
                (long) (MEMORY * MEMORY_OVERCOMMIT * memoryAllocatedFraction), MEMORY));
        vmCounts.put(id, new Ternary<>(vms, 0L, startingVms));
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

    @Test
    public void testHostWithTooManyVmsStartingIsHeldBack() {
        // the emptiest host in the cluster by every other measure, but already working through a
        // queue of starts: the next deployment must not join that queue
        Host queueing = host(1L, "queueing", 0.05, 0.05, new HostLoad(0.05, 0.05, 10), 5, 12);
        Host busier = host(2L, "busier", 0.60, 0.60, new HostLoad(0.50, 0.50, 10), 60, 0);

        List<String> ranked = rankedNames(Arrays.asList(queueing, busier));

        assertEquals("busier", ranked.get(0));
        assertEquals("queueing", ranked.get(1));
    }

    @Test
    public void testHostBelowTheStartingThresholdIsNotHeldBack() {
        Host starting = host(1L, "starting", 0.05, 0.05, new HostLoad(0.05, 0.05, 10), 5, 9);
        Host busier = host(2L, "busier", 0.60, 0.60, new HostLoad(0.50, 0.50, 10), 60, 0);

        assertEquals("nine starts is under the default threshold of ten",
                "starting", rankedNames(Arrays.asList(starting, busier)).get(0));
    }

    @Test
    public void testEveryHostBackedUpStillDeploys() {
        // nothing is eligible, but a deployment must still be placed rather than failing. The
        // held-back group is ordered by starting count rather than by score, so the most backed up
        // host stays last even though it is the cheapest on every other measure.
        Host worst = host(1L, "worst", 0.10, 0.10, new HostLoad(0.10, 0.10, 10), 10, 40);
        Host least = host(2L, "least", 0.80, 0.80, new HostLoad(0.60, 0.60, 10), 80, 11);
        Host middle = host(3L, "middle", 0.10, 0.10, new HostLoad(0.10, 0.10, 10), 10, 20);
        Host nearly = host(4L, "nearly", 0.10, 0.10, new HostLoad(0.10, 0.10, 10), 10, 25);

        List<Host> input = Arrays.asList(worst, least, middle, nearly);

        for (int attempt = 0; attempt < 200; attempt++) {
            List<String> ranked = rankedNames(input);
            assertEquals("every host must still be offered", 4, ranked.size());
            assertEquals("the most backed up host must stay last", "worst", ranked.get(3));
            assertFalse("the most backed up host must never lead", "worst".equals(ranked.get(0)));
        }
    }

    @Test
    public void testStartingThresholdOfZeroDisablesTheGate() {
        scorer = new WeightedHostScorer() {
            @Override
            protected int startingVmsThreshold(Long clusterId) {
                return 0;
            }
        };
        ReflectionTestUtils.setField(scorer, "capacityDao", capacityDao);
        ReflectionTestUtils.setField(scorer, "clusterDetailsDao", clusterDetailsDao);
        ReflectionTestUtils.setField(scorer, "vmInstanceDao", vmInstanceDao);
        ReflectionTestUtils.setField(scorer, "hostLoadTracker", hostLoadTracker);
        scorer.random = new Random(1L);

        Host queueing = host(1L, "queueing", 0.05, 0.05, new HostLoad(0.05, 0.05, 10), 5, 50);
        Host busier = host(2L, "busier", 0.60, 0.60, new HostLoad(0.50, 0.50, 10), 60, 0);

        assertEquals("with the gate disabled only the score matters",
                "queueing", rankedNames(Arrays.asList(queueing, busier)).get(0));
    }

    @Test
    public void testSpreadStillAppliesWhenNoHostIsEligible() {
        // regression: the spread used to be applied to the healthy list only. With every host held
        // back, healthy was empty, the shuffle was a no-op and concurrent deployments fell through
        // in strict score order - agreeing on one host in exactly the conditions that created the
        // pile-up in the first place.
        List<Host> input = Arrays.asList(
                host(1L, "a", 0.30, 0.30, new HostLoad(0.10, 0.10, 10), 30, 20),
                host(2L, "b", 0.31, 0.30, new HostLoad(0.10, 0.10, 10), 30, 20),
                host(3L, "c", 0.32, 0.30, new HostLoad(0.10, 0.10, 10), 30, 20));

        Set<String> leaders = new HashSet<>();
        for (int attempt = 0; attempt < 200; attempt++) {
            leaders.add(rankedNames(input).get(0));
        }

        assertTrue("held-back hosts must still be spread over, not handed out in strict order",
                leaders.size() > 1);
    }

    @Test
    public void testUnmeasuredHostIsPreferredOverAResourceSaturatedOne() {
        // regression for the same branch: with healthy empty but an unmeasured host present, the
        // ordering between the remaining tiers must still hold
        Host saturated = host(1L, "saturated", 0.95, 0.95, new HostLoad(0.99, 0.99, 10), 95);
        Host unknown = host(2L, "unknown", 0.10, 0.10, HostLoad.UNKNOWN, 10);

        List<String> ranked = rankedNames(Arrays.asList(saturated, unknown));

        assertEquals("unknown", ranked.get(0));
        assertEquals("saturated", ranked.get(1));
    }
}
