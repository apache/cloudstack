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
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Simulates placement over a churning, heavily overprovisioned fleet.
 *
 * This is the regression fixture for the failure the weighted scoring exists to prevent. Ranking
 * hosts on allocated capacity alone is blind to what a host is really doing, and under a large
 * overprovisioning factor the gap between the two can be enormous: a host can report a few percent
 * allocated while its cores are saturated. Anything the scheduler is not told about - VMs it has
 * lost track of, guests using far more than their share - is invisible, so the emptiest looking
 * host keeps being chosen no matter how hard it is working.
 *
 * Three things are modelled that a single-shot unit test cannot show:
 *
 *  - deployments arrive in concurrent batches and every decision in a batch reads the same figures,
 *    because capacity is only charged once a VM starts
 *  - real load that allocation cannot account for
 *  - VMs are short lived and their lifetimes vary, so hosts empty unevenly
 *
 * Deterministic: fixed seeds, no wall clock.
 */
public class WeightedPlacementDistributionTest {

    private static final int HOSTS = 9;
    private static final int CORES_PER_HOST = 192;
    private static final int MEMORY_MB_PER_HOST = 1_132_000;
    private static final int CPU_OVERCOMMIT = 10;
    private static final int MEMORY_OVERCOMMIT = 4;

    private static final int VM_CORES = 4;
    private static final int VM_MEMORY_MB = 8_192;

    private static final int BATCHES = 300;
    private static final int VMS_PER_BATCH = 8;
    private static final int SPREAD = 3;

    /** Share of VMs that peg their cores for their whole life, as build runners do. */
    private static final double BUSY_FRACTION = 0.35;

    /**
     * Cores in use on some hosts that allocation knows nothing about. Stands in for anything the
     * scheduler cannot see - VMs it believes are gone, or guests far exceeding their request.
     */
    private static final int UNACCOUNTED_CORES = 60;

    private static final class SimHost {
        final long id;
        final double unaccountedCores;
        int vms;
        int recentStarts;
        double busyCores;
        double memoryMb;

        SimHost(long id, double unaccountedCores) {
            this.id = id;
            this.unaccountedCores = unaccountedCores;
        }

        /** What the capacity tables would report: uniform per VM, against an inflated total. */
        double cpuAllocated() {
            return (double) vms * VM_CORES / (CORES_PER_HOST * CPU_OVERCOMMIT);
        }

        double memoryAllocated() {
            return (double) vms * VM_MEMORY_MB / ((double) MEMORY_MB_PER_HOST * MEMORY_OVERCOMMIT);
        }

        /** What the host is really doing, including what allocation cannot see. */
        double realCores() {
            return busyCores + unaccountedCores;
        }

        HostLoad load() {
            return new HostLoad(Math.min(1, realCores() / CORES_PER_HOST),
                    Math.min(1, memoryMb / MEMORY_MB_PER_HOST), 10);
        }
    }

    /** Frozen figures for one batch, so every decision in the batch sees the same thing. */
    private static final class HostView {
        final SimHost host;
        final double cpuAllocated;
        final double memoryAllocated;
        final HostLoad load;
        final int vms;
        final int recentStarts;

        HostView(SimHost host) {
            this.host = host;
            this.cpuAllocated = host.cpuAllocated();
            this.memoryAllocated = host.memoryAllocated();
            this.load = host.load();
            this.vms = host.vms;
            this.recentStarts = host.recentStarts;
        }
    }

    private interface Placement {
        SimHost choose(List<HostView> snapshot, Random random);
    }

    /** What firstfitleastconsumed does today: strictly the lowest allocated fraction. */
    private static final Placement LEAST_ALLOCATED = (snapshot, random) ->
            snapshot.stream().min(Comparator.comparingDouble(v -> v.cpuAllocated)).orElseThrow().host;

    /**
     * Allocation-only ranking with the same random spread as the weighted arm. Isolates what the
     * scoring contributes from what the spread alone contributes.
     */
    private static final Placement LEAST_ALLOCATED_WITH_SPREAD = (snapshot, random) -> {
        List<HostView> ranked = new ArrayList<>(snapshot);
        ranked.sort(Comparator.comparingDouble(v -> v.cpuAllocated));
        return ranked.get(random.nextInt(Math.min(SPREAD, ranked.size()))).host;
    };

    private Placement weighted() {
        WeightedHostScorer scorer = new WeightedHostScorer();
        return (snapshot, random) -> {
            List<HostView> ranked = new ArrayList<>(snapshot);
            ranked.sort(Comparator.comparingDouble(v ->
                    scorer.scoreHostIn(null, v.cpuAllocated, v.memoryAllocated, v.load, v.vms, v.recentStarts)));
            return ranked.get(random.nextInt(Math.min(SPREAD, ranked.size()))).host;
        };
    }

    private static final class Result {
        final double[] vmCounts;
        final double[] realCores;

        Result(double[] vmCounts, double[] realCores) {
            this.vmCounts = vmCounts;
            this.realCores = realCores;
        }

        double vmSkew() {
            return max(vmCounts) / mean(vmCounts);
        }

        double loadSkew() {
            return max(realCores) / mean(realCores);
        }
    }

    /** The VMs to be placed, fixed before any arm runs so all arms see the same workload. */
    private List<int[]> workload(long seed) {
        Random random = new Random(seed);
        List<int[]> vms = new ArrayList<>();
        for (int i = 0; i < BATCHES * VMS_PER_BATCH; i++) {
            vms.add(new int[] {random.nextDouble() < BUSY_FRACTION ? 1 : 0, 10 + random.nextInt(50)});
        }
        return vms;
    }

    private Result run(Placement placement, long seed) {
        List<int[]> workload = workload(seed);
        // a separate stream for placement decisions, so arms that consult it differently still see
        // the same workload
        Random random = new Random(seed ^ 0x5DEECE66DL);
        int next = 0;
        List<SimHost> hosts = new ArrayList<>();
        for (int i = 0; i < HOSTS; i++) {
            // a third of the fleet carries load the scheduler cannot account for
            hosts.add(new SimHost(i + 1, i % 3 == 0 ? UNACCOUNTED_CORES : 0));
        }
        List<int[]> live = new ArrayList<>();     // {hostIndex, busy, batchesLeft}

        for (int batch = 0; batch < BATCHES; batch++) {
            live.removeIf(vm -> {
                if (--vm[2] > 0) {
                    return false;
                }
                SimHost host = hosts.get(vm[0]);
                host.vms--;
                host.memoryMb -= VM_MEMORY_MB;
                if (vm[1] == 1) {
                    host.busyCores -= VM_CORES;
                }
                return true;
            });

            hosts.forEach(h -> h.recentStarts = 0);

            List<HostView> snapshot = new ArrayList<>();
            for (SimHost host : hosts) {
                snapshot.add(new HostView(host));
            }

            for (int i = 0; i < VMS_PER_BATCH; i++) {
                SimHost chosen = placement.choose(snapshot, random);
                int[] vm = workload.get(next++);
                boolean busy = vm[0] == 1;
                chosen.vms++;
                chosen.recentStarts++;
                chosen.memoryMb += VM_MEMORY_MB;
                if (busy) {
                    chosen.busyCores += VM_CORES;
                }
                // lifetimes vary, so hosts do not empty in the order they filled
                live.add(new int[] {hosts.indexOf(chosen), vm[0], vm[1]});
            }
        }

        return new Result(hosts.stream().mapToDouble(h -> h.vms).toArray(),
                hosts.stream().mapToDouble(SimHost::realCores).toArray());
    }

    private static double max(double[] values) {
        return Arrays.stream(values).max().orElse(0);
    }

    private static double mean(double[] values) {
        return Arrays.stream(values).average().orElse(0);
    }

    @Test
    public void testAllocationOnlyOrderingPilesRealLoadOntoTheBusiestHosts() {
        Result result = run(LEAST_ALLOCATED, 42L);

        assertTrue(String.format("expected allocation-only ranking to be blind to real load, "
                        + "got cores %s (max/mean %.2f)", Arrays.toString(result.realCores), result.loadSkew()),
                result.loadSkew() > 1.4);
    }

    @Test
    public void testWeightedScoringKeepsRealLoadEven() {
        Result result = run(weighted(), 42L);

        // a third of the fleet carries a fixed handicap the scheduler can only stop adding to, not
        // remove, so some residual skew is expected. Measured across seeds: allocation-only ranking
        // lands at 1.84 to 2.01, weighted at 1.27 to 1.40.
        assertTrue(String.format("real load should be spread, got cores %s (max/mean %.2f)",
                        Arrays.toString(result.realCores), result.loadSkew()),
                result.loadSkew() < 1.5);
    }

    @Test
    public void testWeightedScoringBeatsAllocationOnlyOnEverySeed() {
        for (long seed : new long[] {1L, 7L, 42L, 99L, 12345L}) {
            double baseline = run(LEAST_ALLOCATED, seed).loadSkew();
            double improved = run(weighted(), seed).loadSkew();
            assertTrue(String.format("seed %d: weighted %.2f should beat allocation-only %.2f",
                            seed, improved, baseline),
                    improved < baseline);
        }
    }

    @Test
    public void testTheScoringNotJustTheSpreadIsWhatEvensOutRealLoad() {
        // the control: same random spread, ranking still blind to real load. If the spread alone
        // were doing the work, this arm would do as well as the weighted one.
        for (long seed : new long[] {1L, 42L, 12345L}) {
            double spreadOnly = run(LEAST_ALLOCATED_WITH_SPREAD, seed).loadSkew();
            double weighted = run(weighted(), seed).loadSkew();
            assertTrue(String.format("seed %d: weighted %.2f should beat spread-only %.2f",
                            seed, weighted, spreadOnly),
                    weighted < spreadOnly);
        }
    }

    @Test
    public void testWeightedScoringGivesFewerVmsToHostsCarryingHiddenLoad() {
        Result result = run(weighted(), 42L);

        // hosts 0, 3 and 6 carry load that allocation cannot see, so they should get fewer VMs.
        // uneven VM counts are the right answer here - it is real load that should come out even.
        double withHiddenLoad = mean(new double[] {result.vmCounts[0], result.vmCounts[3], result.vmCounts[6]});
        double withoutHiddenLoad = mean(new double[] {result.vmCounts[1], result.vmCounts[2], result.vmCounts[4],
                result.vmCounts[5], result.vmCounts[7], result.vmCounts[8]});

        assertTrue(String.format("hosts with hidden load should take fewer VMs: %.1f vs %.1f (counts %s)",
                        withHiddenLoad, withoutHiddenLoad, Arrays.toString(result.vmCounts)),
                withHiddenLoad < withoutHiddenLoad * 0.75);
        assertTrue("no host should be left completely unused: " + Arrays.toString(result.vmCounts),
                min(result.vmCounts) > 0);
    }

    @Test
    public void testAllocationOnlyOrderingIgnoresHiddenLoadEntirely() {
        Result result = run(LEAST_ALLOCATED, 42L);

        double withHiddenLoad = mean(new double[] {result.vmCounts[0], result.vmCounts[3], result.vmCounts[6]});
        double withoutHiddenLoad = mean(new double[] {result.vmCounts[1], result.vmCounts[2], result.vmCounts[4],
                result.vmCounts[5], result.vmCounts[7], result.vmCounts[8]});

        assertTrue(String.format("allocation-only ranking should treat loaded and idle hosts alike, got %.1f vs %.1f",
                        withHiddenLoad, withoutHiddenLoad),
                withHiddenLoad > withoutHiddenLoad * 0.9);
    }

    private static double min(double[] values) {
        return Arrays.stream(values).min().orElse(0);
    }
}
