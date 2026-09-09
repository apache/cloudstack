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
import com.cloud.host.Host;
import com.cloud.host.HostScoringWeights;
import com.cloud.utils.component.AdapterBase;
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

    @Inject
    private CapacityDao capacityDao;

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

        List<Host> scored = hosts.stream().filter(h -> scores.containsKey(h.getId())).collect(Collectors.toList());
        List<Host> unscored = hosts.stream().filter(h -> !scores.containsKey(h.getId())).collect(Collectors.toList());
        scored.sort((a, b) -> Double.compare(scores.get(a.getId()), scores.get(b.getId())));

        List<Host> admitted = applyUtilisationThresholds(clusterId, scored);
        applySelectionSpread(clusterId, admitted);

        logger.debug("Weighted host ranking: {}", () -> admitted.stream()
                .map(h -> String.format("%s=%.4f", h.getName(), scores.get(h.getId())))
                .collect(Collectors.joining(", ")));

        admitted.addAll(unscored);
        return admitted;
    }

    protected Map<Long, Double> score(long zoneId, Long podId, Long clusterId, List<? extends Host> hosts) {
        List<CapacityVO> capacities = capacityDao.listHostCapacityByCapacityTypes(zoneId, clusterId,
                List.of(Capacity.CAPACITY_TYPE_CPU, Capacity.CAPACITY_TYPE_MEMORY));
        Map<Long, Long> vmCounts = vmInstanceDao.countVmsByHost(zoneId, podId, clusterId, null);
        Map<Long, Long> recentStarts = vmInstanceDao.countVmsByHost(zoneId, podId, clusterId,
                new Date(System.currentTimeMillis() - RecentStartWindow.value() * 1000L));

        Map<Long, Double[]> allocated = allocatedFractions(capacities);

        Map<Long, Double> scores = new HashMap<>();
        for (Host host : hosts) {
            Double[] alloc = allocated.get(host.getId());
            if (alloc == null) {
                continue;
            }
            scores.put(host.getId(), scoreHost(clusterId, alloc[0], alloc[1], hostLoadTracker.getLoad(host.getId()),
                    vmCounts.getOrDefault(host.getId(), 0L), recentStarts.getOrDefault(host.getId(), 0L)));
        }
        return scores;
    }

    /**
     * Allocated CPU and memory as a fraction of what the host advertises after overprovisioning,
     * which is the same basis the existing allocators use.
     */
    protected Map<Long, Double[]> allocatedFractions(List<CapacityVO> capacities) {
        Map<Long, Double[]> fractions = new HashMap<>();
        for (CapacityVO capacity : capacities) {
            long total = capacity.getTotalCapacity();
            if (total <= 0) {
                continue;
            }
            double used = (double) (capacity.getUsedCapacity() + capacity.getReservedCapacity()) / total;
            Double[] entry = fractions.computeIfAbsent(capacity.getHostOrPoolId(), id -> new Double[] {0.0, 0.0});
            if (capacity.getCapacityType() == Capacity.CAPACITY_TYPE_CPU) {
                entry[0] = clamp(used);
            } else {
                entry[1] = clamp(used);
            }
        }
        return fractions;
    }

    /**
     * The blend. Every term is a fraction of the host's capacity for that resource so the weights
     * are directly comparable, and the dominant resource term is added on top of the weighted mean
     * so that being nearly out of any one resource is penalised even when the average looks fine.
     */
    protected double scoreHost(Long clusterId, double cpuAllocated, double memoryAllocated, HostLoad load,
            long vmCount, long recentStarts) {
        double cpuUsedWeight = load.isUsable() ? valueIn(HostScoringWeights.CpuUsedWeight, clusterId) : 0;
        double memoryUsedWeight = load.isUsable() ? valueIn(HostScoringWeights.MemoryUsedWeight, clusterId) : 0;
        double vmScale = Math.max(1, valueIn(ExpectedVmsPerHost, clusterId));

        double cpuAllocatedWeight = valueIn(HostScoringWeights.CpuAllocatedWeight, clusterId);
        double memoryAllocatedWeight = valueIn(HostScoringWeights.MemoryAllocatedWeight, clusterId);
        double vmCountWeight = valueIn(VmCountWeight, clusterId);
        double recentStartWeight = valueIn(RecentStartWeight, clusterId);

        double vmCountTerm = clamp(vmCount / vmScale);
        double recentStartTerm = clamp(recentStarts / vmScale);

        double weightSum = cpuAllocatedWeight + cpuUsedWeight + memoryAllocatedWeight + memoryUsedWeight
                + vmCountWeight + recentStartWeight;
        if (weightSum <= 0) {
            return 0;
        }

        double weighted = cpuAllocatedWeight * cpuAllocated
                + cpuUsedWeight * load.getCpuUtilisation()
                + memoryAllocatedWeight * memoryAllocated
                + memoryUsedWeight * load.getMemoryUtilisation()
                + vmCountWeight * vmCountTerm
                + recentStartWeight * recentStartTerm;

        double mean = weighted / weightSum;
        double dominantWeight = valueIn(DominantResourceWeight, clusterId);
        if (dominantWeight <= 0) {
            return mean;
        }
        return (mean + dominantWeight * dominantResource(cpuAllocated, memoryAllocated, load)) / (1 + dominantWeight);
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
     * Holds back hosts that are measurably too busy, unless that would leave nothing to deploy on,
     * in which case ranking alone decides and the caller's capacity checks still apply.
     */
    protected List<Host> applyUtilisationThresholds(Long clusterId, List<Host> ranked) {
        double cpuThreshold = valueIn(CpuUtilisationThreshold, clusterId);
        double memoryThreshold = valueIn(MemoryUtilisationThreshold, clusterId);

        List<Host> admitted = new ArrayList<>();
        List<Host> heldBack = new ArrayList<>();
        for (Host host : ranked) {
            HostLoad load = hostLoadTracker.getLoad(host.getId());
            if (load.isUsable()
                    && (load.getCpuUtilisation() > cpuThreshold || load.getMemoryUtilisation() > memoryThreshold)) {
                heldBack.add(host);
            } else {
                admitted.add(host);
            }
        }

        if (admitted.isEmpty()) {
            logger.warn("Every candidate host is above its utilisation threshold, so the thresholds are being "
                    + "ignored for this deployment. The cluster is short of capacity.");
            return heldBack;
        }
        if (!heldBack.isEmpty()) {
            logger.debug("Holding back {} host(s) above their utilisation threshold: {}", heldBack.size(), heldBack);
            admitted.addAll(heldBack);
        }
        return admitted;
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
