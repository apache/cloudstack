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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityManager;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.host.Host;
import com.cloud.host.HostLoad;
import com.cloud.host.HostScoringWeights;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.VMInstanceDao;

/**
 * Ranks hosts on a blend of what has been allocated on them and what they are actually doing.
 *
 * Ordering purely by allocated capacity misreads a heavily overprovisioned cluster: allocation is
 * measured against a total that has been multiplied by the overprovisioning factor, so hosts under
 * real strain can still look close to empty and keep attracting new VMs. This blends allocation
 * with measured utilisation, VM count, and how many VMs started on the host recently, since a VM
 * that has just started is usually working harder than its long run average.
 *
 * Scores run from 0 (idle) upwards and lower is better. Hosts are then chosen from among the best
 * rather than strictly in order - see {@link #applySelectionSpread}.
 */
public class WeightedHostScorer extends AdapterBase implements Configurable {

    private static final String WEIGHT_DESCRIPTION_SUFFIX =
            " Relative weight, only meaningful compared with the other host.weighted.* weights. Zero disables the term.";

    public static final ConfigKey<Double> VmCountWeight = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            Double.class, "host.weighted.vm.count.weight", "1.0",
            "How much the number of VMs already on a host counts against it, regardless of how busy they are."
                    + WEIGHT_DESCRIPTION_SUFFIX,
            true, ConfigKey.Scope.Cluster);

    public static final ConfigKey<Double> RecentStartWeight = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            Double.class, "host.weighted.recent.start.weight", "2.0",
            "How much VMs started recently on a host count against it. Guards against sending a burst of new "
                    + "VMs to one host, since neither allocation nor utilisation has caught up with them yet."
                    + WEIGHT_DESCRIPTION_SUFFIX,
            true, ConfigKey.Scope.Cluster);

    public static final ConfigKey<Double> DominantResourceWeight = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            Double.class, "host.weighted.dominant.resource.weight", "1.0",
            "How much a host's single most stressed resource counts against it, on top of the average across "
                    + "resources. Keeps a host that is fine on average but nearly out of one resource from ranking well."
                    + WEIGHT_DESCRIPTION_SUFFIX,
            true, ConfigKey.Scope.Cluster);

    public static final ConfigKey<Integer> RecentStartWindow = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            Integer.class, "host.weighted.recent.start.window", "300",
            "Seconds for which a newly started VM counts as recently started.",
            true, ConfigKey.Scope.Global);

    public static final ConfigKey<Integer> ExpectedVmsPerHost = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            Integer.class, "host.weighted.expected.vms.per.host", "50",
            "Roughly how many VMs a host is expected to carry. Used only to bring VM counts onto the same "
                    + "0 to 1 scale as the other terms; it is not a limit and is never enforced.",
            true, ConfigKey.Scope.Cluster);

    public static final ConfigKey<Double> CpuUtilisationThreshold = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            Double.class, "host.weighted.cpu.utilisation.threshold", "0.85",
            "Hosts whose measured CPU utilisation is above this fraction are held back from new VMs. "
                    + "Ignored if it would leave nowhere to deploy. Set to 1 to disable.",
            true, ConfigKey.Scope.Cluster);

    public static final ConfigKey<Double> MemoryUtilisationThreshold = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            Double.class, "host.weighted.memory.utilisation.threshold", "0.90",
            "Hosts whose measured memory utilisation is above this fraction are held back from new VMs. "
                    + "Ignored if it would leave nowhere to deploy. Set to 1 to disable.",
            true, ConfigKey.Scope.Cluster);

    public static final ConfigKey<Integer> SelectionSpread = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            Integer.class, "host.weighted.selection.spread", "3",
            "How many of the best scoring hosts to choose between at random. Ranking strictly by score sends "
                    + "concurrent deployments to the same host, because they all read the same figures before any "
                    + "of them is accounted for. 1 restores strict ordering.",
            true, ConfigKey.Scope.Cluster);

    private static final Pair<Long, Long> NO_VMS = new Pair<>(0L, 0L);

    @Inject
    private CapacityDao capacityDao;

    @Inject
    private ClusterDetailsDao clusterDetailsDao;

    @Inject
    private VMInstanceDao vmInstanceDao;

    @Inject
    private HostLoadTracker hostLoadTracker;

    protected Random random = new Random();

    /**
     * Orders hosts best first. Hosts absent from the capacity tables keep their original relative
     * order at the end of the list rather than being dropped.
     */
    public List<Host> rank(long zoneId, Long podId, Long clusterId, List<? extends Host> hosts) {
        if (hosts == null || hosts.size() <= 1) {
            return hosts == null ? new ArrayList<>() : new ArrayList<>(hosts);
        }

        Map<Long, Double> scores = score(zoneId, podId, clusterId, hosts);

        List<Host> unscored = new ArrayList<>();
        List<Host> measured = new ArrayList<>();
        List<Host> unmeasured = new ArrayList<>();
        for (Host host : hosts) {
            if (!scores.containsKey(host.getId())) {
                unscored.add(host);
            } else if (hostLoadTracker.getLoad(host.getId()).isUsable()) {
                measured.add(host);
            } else {
                unmeasured.add(host);
            }
        }

        Comparator<Host> byScore = Comparator.comparingDouble(h -> scores.get(h.getId()));
        measured.sort(byScore);
        unmeasured.sort(byScore);

        List<Host> healthy = new ArrayList<>();
        List<Host> tooBusy = new ArrayList<>();
        partitionByUtilisation(clusterId, measured, healthy, tooBusy);

        List<Host> result = new ArrayList<>();
        if (healthy.isEmpty() && unmeasured.isEmpty()) {
            logger.warn("Every candidate host is above its utilisation threshold, so the thresholds are being "
                    + "ignored for this deployment. The cluster is short of capacity.");
            result.addAll(tooBusy);
            applySelectionSpread(clusterId, result);
        } else {
            result.addAll(healthy);
            // spread only over hosts known to be healthy, before anything else is appended,
            // otherwise a busy or unmeasured host can be shuffled into the lead
            applySelectionSpread(clusterId, result);
            // a host we cannot measure is not assumed to be idle: it ranks behind every host we can
            result.addAll(unmeasured);
            result.addAll(tooBusy);
        }

        if (!tooBusy.isEmpty()) {
            logger.debug("Holding back {} host(s) above their utilisation threshold: {}", tooBusy.size(), tooBusy);
        }
        logger.debug("Weighted host ranking: {}", () -> result.stream()
                .filter(h -> scores.containsKey(h.getId()))
                .map(h -> String.format("%s=%.4f", h.getName(), scores.get(h.getId())))
                .collect(Collectors.joining(", ")));

        result.addAll(unscored);
        return result;
    }

    protected Map<Long, Double> score(long zoneId, Long podId, Long clusterId, List<? extends Host> hosts) {
        List<CapacityVO> capacities = capacityDao.listHostCapacityByCapacityTypes(zoneId, clusterId,
                List.of(Capacity.CAPACITY_TYPE_CPU, Capacity.CAPACITY_TYPE_MEMORY));
        Map<Long, Pair<Long, Long>> vmCounts = vmInstanceDao.countVmsByHost(zoneId, podId, clusterId,
                new Date(System.currentTimeMillis() - RecentStartWindow.value() * 1000L));

        Map<Long, Double[]> allocated = allocatedFractions(capacities);

        Weights weights = new Weights(clusterId);
        Map<Long, Double> scores = new HashMap<>();
        for (Host host : hosts) {
            Double[] alloc = allocated.get(host.getId());
            if (alloc == null) {
                continue;
            }
            Pair<Long, Long> counts = vmCounts.getOrDefault(host.getId(), NO_VMS);
            scores.put(host.getId(), scoreHost(weights, alloc[0], alloc[1], hostLoadTracker.getLoad(host.getId()),
                    counts.first(), counts.second()));
        }
        return scores;
    }

    /**
     * Allocated CPU and memory as a fraction of what a host can hand out.
     *
     * op_host_capacity stores totals raw; overprovisioning is applied when they are read, so the
     * cluster's ratio has to be applied here too. Without it the fraction reaches 1 at the host's
     * physical size and every host on an overcommitted cluster clamps to 1, which is where this
     * algorithm is most needed.
     *
     * Only hosts with both a CPU and a memory row are returned. A host missing one would otherwise
     * score as if that resource were untouched, making it the most attractive host in the cluster.
     */
    protected Map<Long, Double[]> allocatedFractions(List<CapacityVO> capacities) {
        Map<Long, Double[]> fractions = new HashMap<>();
        Map<Long, Integer> seen = new HashMap<>();
        for (CapacityVO capacity : capacities) {
            long total = capacity.getTotalCapacity();
            if (total <= 0) {
                continue;
            }
            boolean isCpu = capacity.getCapacityType() == Capacity.CAPACITY_TYPE_CPU;
            float overcommit = overcommitRatio(capacity.getClusterId(), isCpu);
            double allocatable = total * overcommit;
            double used = (double) (capacity.getUsedCapacity() + capacity.getReservedCapacity()) / allocatable;

            Double[] entry = fractions.computeIfAbsent(capacity.getHostOrPoolId(), id -> new Double[] {0.0, 0.0});
            entry[isCpu ? 0 : 1] = clamp(used);
            seen.merge(capacity.getHostOrPoolId(), isCpu ? 1 : 2, Integer::sum);
        }
        fractions.keySet().removeIf(hostId -> seen.getOrDefault(hostId, 0) != 3);
        return fractions;
    }

    /**
     * The cluster's overprovisioning factor, defaulting to none if it cannot be read.
     */
    protected float overcommitRatio(Long clusterId, boolean forCpu) {
        if (clusterId == null) {
            return 1f;
        }
        String key = forCpu ? VmDetailConstants.CPU_OVER_COMMIT_RATIO : VmDetailConstants.MEMORY_OVER_COMMIT_RATIO;
        ClusterDetailsVO detail = clusterDetailsDao.findDetail(clusterId, key);
        if (detail == null || detail.getValue() == null) {
            return 1f;
        }
        try {
            float ratio = Float.parseFloat(detail.getValue());
            return ratio > 0 ? ratio : 1f;
        } catch (NumberFormatException e) {
            logger.warn("Cluster {} has an unreadable {} of [{}], treating it as 1.", clusterId, key, detail.getValue());
            return 1f;
        }
    }

    /**
     * The blend. Every term is a fraction of the host's capacity for that resource so the weights
     * are directly comparable, and the dominant resource term is added on top of the weighted mean
     * so that being nearly out of any one resource is penalised even when the average looks fine.
     */
    protected double scoreHostIn(Long clusterId, double cpuAllocated, double memoryAllocated, HostLoad load,
            long vmCount, long recentStarts) {
        return scoreHost(new Weights(clusterId), cpuAllocated, memoryAllocated, load, vmCount, recentStarts);
    }

    protected double scoreHost(Weights weights, double cpuAllocated, double memoryAllocated, HostLoad load,
            long vmCount, long recentStarts) {
        // a host with no usable load figures is ranked on allocation alone, and is placed behind
        // every measured host by the caller rather than being assumed idle
        double cpuUsedWeight = load.isUsable() ? weights.cpuUsed : 0;
        double memoryUsedWeight = load.isUsable() ? weights.memoryUsed : 0;

        double vmCountTerm = clamp(vmCount / weights.vmScale);
        double recentStartTerm = clamp(recentStarts / weights.vmScale);

        double weightSum = weights.cpuAllocated + cpuUsedWeight + weights.memoryAllocated + memoryUsedWeight
                + weights.vmCount + weights.recentStart;

        double mean = 0;
        if (weightSum > 0) {
            mean = (weights.cpuAllocated * cpuAllocated
                    + cpuUsedWeight * load.getCpuUtilisation()
                    + weights.memoryAllocated * memoryAllocated
                    + memoryUsedWeight * load.getMemoryUtilisation()
                    + weights.vmCount * vmCountTerm
                    + weights.recentStart * recentStartTerm) / weightSum;
        }

        if (weights.dominant <= 0) {
            return mean;
        }
        return (mean + weights.dominant * dominantResource(cpuAllocated, memoryAllocated, load)) / (1 + weights.dominant);
    }

    /**
     * The most stressed resource on the host. Allocation and utilisation are both considered for
     * each resource and the larger is taken, because memory that has been reclaimed from idle
     * guests can be taken back as soon as those guests get busy.
     */
    protected double dominantResource(double cpuAllocated, double memoryAllocated, HostLoad load) {
        double cpu = load.isUsable() ? Math.max(cpuAllocated, load.getCpuUtilisation()) : cpuAllocated;
        double memory = load.isUsable() ? Math.max(memoryAllocated, load.getMemoryUtilisation()) : memoryAllocated;
        return Math.max(cpu, memory);
    }

    /**
     * Splits measurably busy hosts out from the rest. Only hosts with load samples can be held
     * back; a host that cannot be measured is dealt with by the caller.
     */
    protected void partitionByUtilisation(Long clusterId, List<Host> measured, List<Host> healthy, List<Host> tooBusy) {
        double cpuThreshold = valueIn(CpuUtilisationThreshold, clusterId);
        double memoryThreshold = valueIn(MemoryUtilisationThreshold, clusterId);

        for (Host host : measured) {
            HostLoad load = hostLoadTracker.getLoad(host.getId());
            if (load.getCpuUtilisation() > cpuThreshold || load.getMemoryUtilisation() > memoryThreshold) {
                tooBusy.add(host);
            } else {
                healthy.add(host);
            }
        }
    }

    /**
     * Shuffles the best few hosts so that deployments made at the same moment do not all pick the
     * same one. Capacity is only charged once a VM starts, so until then every concurrent decision
     * sees the same figures and strict ordering makes them agree.
     */
    protected void applySelectionSpread(Long clusterId, List<Host> ranked) {
        int spread = Math.min((int) valueIn(SelectionSpread, clusterId), ranked.size());
        if (spread > 1) {
            Collections.shuffle(ranked.subList(0, spread), random);
        }
    }

    /**
     * The weights for one ranking, read once rather than per host.
     *
     * A negative weight would invert the ranking and make the most loaded host the best, so they
     * are floored at zero and the bad value is reported.
     */
    protected final class Weights {
        private final double cpuAllocated;
        private final double cpuUsed;
        private final double memoryAllocated;
        private final double memoryUsed;
        private final double vmCount;
        private final double recentStart;
        private final double dominant;
        private final double vmScale;

        protected Weights(Long clusterId) {
            cpuAllocated = nonNegative(HostScoringWeights.CpuAllocatedWeight, clusterId);
            cpuUsed = nonNegative(HostScoringWeights.CpuUsedWeight, clusterId);
            memoryAllocated = nonNegative(HostScoringWeights.MemoryAllocatedWeight, clusterId);
            memoryUsed = nonNegative(HostScoringWeights.MemoryUsedWeight, clusterId);
            vmCount = nonNegative(VmCountWeight, clusterId);
            recentStart = nonNegative(RecentStartWeight, clusterId);
            dominant = nonNegative(DominantResourceWeight, clusterId);
            vmScale = Math.max(1, valueIn(ExpectedVmsPerHost, clusterId));
        }

        private double nonNegative(ConfigKey<Double> key, Long clusterId) {
            double value = valueIn(key, clusterId);
            if (value < 0) {
                logger.warn("{} is set to {}, which would rank the most loaded host first. Treating it as 0.",
                        key.key(), value);
                return 0;
            }
            return value;
        }
    }

    private <T extends Number> double valueIn(ConfigKey<T> key, Long clusterId) {
        T value = clusterId == null ? key.value() : key.valueIn(clusterId);
        return value == null ? 0 : value.doubleValue();
    }

    private static double clamp(double value) {
        if (Double.isNaN(value) || value < 0) {
            return 0;
        }
        return Math.min(value, 1);
    }

    @Override
    public String getConfigComponentName() {
        return CapacityManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {HostScoringWeights.CpuAllocatedWeight, HostScoringWeights.CpuUsedWeight, HostScoringWeights.MemoryAllocatedWeight, HostScoringWeights.MemoryUsedWeight,
                VmCountWeight, RecentStartWeight, DominantResourceWeight, RecentStartWindow, ExpectedVmsPerHost,
                CpuUtilisationThreshold, MemoryUtilisationThreshold, SelectionSpread};
    }
}
