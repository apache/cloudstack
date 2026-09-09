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
package org.apache.cloudstack.cluster;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.host.Host;
import com.cloud.host.HostLoad;
import com.cloud.host.HostScoringWeights;
import com.cloud.host.HostLoadService;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmDetailConstants;

/**
 * Balances a cluster on CPU and memory together, and on what hosts are really doing rather than
 * only on what has been allocated to them.
 *
 * The existing algorithms balance a single metric chosen by drs.metric. Choosing one leaves the
 * other unwatched: a cluster can be even on memory while its CPU load varies several fold, and
 * nothing moves. Allocation is also a poor stand-in for load under overprovisioning, where a
 * saturated host can still report a small percentage allocated.
 *
 * This blends four figures per host - CPU and memory allocated, CPU and memory in use - and
 * balances the result. The weights are the same host.weighted.* settings initial placement uses, on
 * purpose: if the two weighted them differently they would disagree about which host is the better
 * one, and rebalancing could move VMs off hosts that placement had just chosen. Imbalance keeps the same meaning as the other algorithms: the standard
 * deviation of the per-host figure over its mean, so drs.imbalance still means what it did.
 */
public class Weighted extends AdapterBase implements ClusterDrsAlgorithm, Configurable {

    private static final Logger LOGGER = LogManager.getLogger(Weighted.class);

    @Inject
    private HostLoadService hostLoadService;

    @Inject
    private ClusterDetailsDao clusterDetailsDao;

    @Override
    public String getName() {
        return "weighted";
    }

    @Override
    public boolean needsDrs(Cluster cluster, List<Ternary<Long, Long, Long>> cpuList,
            List<Ternary<Long, Long, Long>> memoryList) throws ConfigurationException {
        // without host identity, measured load cannot be attributed; the map form is what DRS calls
        Map<Long, Ternary<Long, Long, Long>> cpuMap = new HashMap<>();
        Map<Long, Ternary<Long, Long, Long>> memoryMap = new HashMap<>();
        for (int i = 0; i < cpuList.size() && i < memoryList.size(); i++) {
            cpuMap.put((long) -(i + 1), cpuList.get(i));
            memoryMap.put((long) -(i + 1), memoryList.get(i));
        }
        return needsDrs(cluster, cpuMap, memoryMap);
    }

    @Override
    public boolean needsDrs(Cluster cluster, Map<Long, Ternary<Long, Long, Long>> hostCpuMap,
            Map<Long, Ternary<Long, Long, Long>> hostMemoryMap) throws ConfigurationException {
        double threshold = 1.0 - ClusterDrsService.ClusterDrsImbalanceThreshold.valueIn(cluster.getId());
        double imbalance = imbalanceOf(blendByHost(cluster, hostCpuMap, hostMemoryMap).values());
        boolean needed = imbalance > threshold;
        LOGGER.debug("Cluster {} {} DRS. Imbalance: {} Threshold: {} Algorithm: {}",
                cluster, needed ? "needs" : "does not need", imbalance, threshold, getName());
        return needed;
    }

    @Override
    public Ternary<Double, Double, Double> getMetrics(Cluster cluster, VirtualMachine vm,
            ServiceOffering serviceOffering, Host destHost,
            Map<Long, Ternary<Long, Long, Long>> hostCpuMap, Map<Long, Ternary<Long, Long, Long>> hostMemoryMap,
            Boolean requiresStorageMotion, Double preImbalance,
            double[] baseMetricsArray, Map<Long, Integer> hostIdToIndexMap) throws ConfigurationException {

        double before = imbalanceOf(blendByHost(cluster, hostCpuMap, hostMemoryMap).values());

        long vmCpu = (long) serviceOffering.getCpu() * serviceOffering.getSpeed();
        long vmMemory = serviceOffering.getRamSize() * 1024L * 1024L;
        Map<Long, Ternary<Long, Long, Long>> cpuAfter = withVmMoved(hostCpuMap, vm.getHostId(), destHost.getId(), vmCpu);
        Map<Long, Ternary<Long, Long, Long>> memoryAfter = withVmMoved(hostMemoryMap, vm.getHostId(), destHost.getId(), vmMemory);

        double after = imbalanceOf(blendByHost(cluster, cpuAfter, memoryAfter).values());

        double improvement = before - after;
        // moving a VM costs something and buys nothing unless the cluster ends up more even, so a
        // migration is only worth making when it measurably helps
        double cost = Boolean.TRUE.equals(requiresStorageMotion) ? 1.0 : 0.0;
        double benefit = improvement > 0 ? 1.0 + improvement : 0.0;

        LOGGER.trace("Cluster {} imbalance {} -> {} moving {} to {}", cluster, before, after, vm, destHost);
        return new Ternary<>(improvement, cost, benefit);
    }

    /**
     * One figure per host, blending what is allocated with what is in use.
     */
    protected Map<Long, Double> blendByHost(Cluster cluster, Map<Long, Ternary<Long, Long, Long>> hostCpuMap,
            Map<Long, Ternary<Long, Long, Long>> hostMemoryMap) {
        float cpuOvercommit = overcommitRatio(cluster.getId(), VmDetailConstants.CPU_OVER_COMMIT_RATIO);
        float memoryOvercommit = overcommitRatio(cluster.getId(), VmDetailConstants.MEMORY_OVER_COMMIT_RATIO);

        double cpuAllocatedWeight = weight(HostScoringWeights.CpuAllocatedWeight, cluster.getId());
        double memoryAllocatedWeight = weight(HostScoringWeights.MemoryAllocatedWeight, cluster.getId());
        double cpuUsedWeight = weight(HostScoringWeights.CpuUsedWeight, cluster.getId());
        double memoryUsedWeight = weight(HostScoringWeights.MemoryUsedWeight, cluster.getId());

        Map<Long, Double> blended = new HashMap<>();
        for (Map.Entry<Long, Ternary<Long, Long, Long>> entry : hostCpuMap.entrySet()) {
            long hostId = entry.getKey();
            Ternary<Long, Long, Long> memory = hostMemoryMap.get(hostId);
            if (memory == null) {
                continue;
            }
            double cpuAllocated = fractionOf(entry.getValue(), cpuOvercommit);
            double memoryAllocated = fractionOf(memory, memoryOvercommit);

            HostLoad load = hostLoadService == null ? HostLoad.UNKNOWN : hostLoadService.getLoad(hostId);
            double usedCpuWeight = load.isUsable() ? cpuUsedWeight : 0;
            double usedMemoryWeight = load.isUsable() ? memoryUsedWeight : 0;

            double sum = cpuAllocatedWeight + memoryAllocatedWeight + usedCpuWeight + usedMemoryWeight;
            if (sum <= 0) {
                blended.put(hostId, 0.0);
                continue;
            }
            blended.put(hostId, (cpuAllocatedWeight * cpuAllocated
                    + memoryAllocatedWeight * memoryAllocated
                    + usedCpuWeight * load.getCpuUtilisation()
                    + usedMemoryWeight * load.getMemoryUtilisation()) / sum);
        }
        return blended;
    }

    private Map<Long, Ternary<Long, Long, Long>> withVmMoved(Map<Long, Ternary<Long, Long, Long>> original,
            Long sourceHostId, long destHostId, long amount) {
        Map<Long, Ternary<Long, Long, Long>> copy = new HashMap<>();
        for (Map.Entry<Long, Ternary<Long, Long, Long>> entry : original.entrySet()) {
            Ternary<Long, Long, Long> value = entry.getValue();
            long used = value.first();
            if (entry.getKey().equals(sourceHostId)) {
                used -= amount;
            } else if (entry.getKey() == destHostId) {
                used += amount;
            }
            copy.put(entry.getKey(), new Ternary<>(used, value.second(), value.third()));
        }
        return copy;
    }

    /**
     * Used over what the host can hand out, which is its real total scaled by the overcommit ratio.
     */
    private double fractionOf(Ternary<Long, Long, Long> capacity, float overcommit) {
        double allocatable = (capacity.third() - capacity.second()) * (double) overcommit;
        if (allocatable <= 0) {
            return 0;
        }
        return clamp(capacity.first() / allocatable);
    }

    protected float overcommitRatio(long clusterId, String key) {
        ClusterDetailsVO detail = clusterDetailsDao.findDetail(clusterId, key);
        if (detail == null || detail.getValue() == null) {
            return 1f;
        }
        try {
            float ratio = Float.parseFloat(detail.getValue());
            return ratio > 0 ? ratio : 1f;
        } catch (NumberFormatException e) {
            return 1f;
        }
    }

    private double weight(ConfigKey<Double> key, long clusterId) {
        Double value = key.valueIn(clusterId);
        if (value == null || value < 0) {
            return 0;
        }
        return value;
    }

    /**
     * Standard deviation over the mean, the same definition the other algorithms use, so that
     * drs.imbalance keeps its meaning.
     */
    protected double imbalanceOf(java.util.Collection<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        double[] array = values.stream().mapToDouble(Double::doubleValue).toArray();
        double mean = MEAN_CALCULATOR.evaluate(array);
        if (mean == 0) {
            return 0;
        }
        return STDDEV_CALCULATOR.evaluate(array, mean) / mean;
    }

    private static double clamp(double value) {
        if (Double.isNaN(value) || value < 0) {
            return 0;
        }
        return Math.min(value, 1);
    }

    @Override
    public String getConfigComponentName() {
        return Weighted.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        // the four host.weighted.* weights are shared with initial placement, which registers them
        return new ConfigKey<?>[] {StorageMotionCost};
    }
}
