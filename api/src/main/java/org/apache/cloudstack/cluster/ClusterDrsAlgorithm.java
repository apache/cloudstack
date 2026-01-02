/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.cluster;

import com.cloud.host.Host;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.Adapter;
import com.cloud.vm.VirtualMachine;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.cloudstack.cluster.ClusterDrsService.ClusterDrsMetric;
import static org.apache.cloudstack.cluster.ClusterDrsService.ClusterDrsMetricType;
import static org.apache.cloudstack.cluster.ClusterDrsService.ClusterDrsMetricUseRatio;

public interface ClusterDrsAlgorithm extends Adapter {

    Mean MEAN_CALCULATOR = new Mean();
    StandardDeviation STDDEV_CALCULATOR = new StandardDeviation(false);

    /**
     * Determines whether a DRS operation is needed for a given cluster and host-VM
     * mapping.
     *
     * @param clusterId
     *         the ID of the cluster to check
     * @param cpuList
     *         a list of Ternary of used, reserved & total CPU for each host in the cluster
     * @param memoryList
     *         a list of Ternary of used, reserved & total memory values for each host in the cluster
     *
     * @return true if a DRS operation is needed, false otherwise
     *
     * @throws ConfigurationException
     *         if there is an error in the configuration
     */
    boolean needsDrs(Cluster cluster, List<Ternary<Long, Long, Long>> cpuList,
                     List<Ternary<Long, Long, Long>> memoryList) throws ConfigurationException;

    /**
     * Calculates the metrics (improvement, cost, benefit) for migrating a VM to a destination host. Improvement is
     * calculated based on the change in cluster imbalance before and after the migration.
     *
     * @param cluster the cluster to check
     * @param vm the virtual machine to check
     * @param serviceOffering the service offering for the virtual machine
     * @param destHost the destination host for the virtual machine
     * @param hostCpuMap a map of host IDs to the Ternary of used, reserved and total CPU on each host
     * @param hostMemoryMap a map of host IDs to the Ternary of used, reserved and total memory on each host
     * @param requiresStorageMotion whether storage motion is required for the virtual machine
     * @param preImbalance the pre-calculated cluster imbalance before migration (null to calculate it)
     * @param baseMetricsArray pre-calculated array of all host metrics before migration
     * @param hostIdToIndexMap mapping from host ID to index in the metrics array
     * @return a ternary containing improvement, cost, benefit
     */
    Ternary<Double, Double, Double> getMetrics(Cluster cluster, VirtualMachine vm, ServiceOffering serviceOffering,
            Host destHost, Map<Long, Ternary<Long, Long, Long>> hostCpuMap,
            Map<Long, Ternary<Long, Long, Long>> hostMemoryMap,
            Boolean requiresStorageMotion, Double preImbalance,
            double[] baseMetricsArray, Map<Long, Integer> hostIdToIndexMap) throws ConfigurationException;

    /**
     * Calculates the cluster imbalance after migrating a VM to a destination host.
     *
     * @param vm the virtual machine being migrated
     * @param destHost the destination host for the virtual machine
     * @param clusterId the cluster ID
     * @param vmMetric the VM's resource consumption metric
     * @param baseMetricsArray pre-calculated array of all host metrics before migration
     * @param hostIdToIndexMap mapping from host ID to index in the metrics array
     * @return the cluster imbalance after migration
     */
    default Double getImbalancePostMigration(VirtualMachine vm,
            Host destHost, Long clusterId, long vmMetric, double[] baseMetricsArray,
            Map<Long, Integer> hostIdToIndexMap, Map<Long, Ternary<Long, Long, Long>> hostCpuMap,
            Map<Long, Ternary<Long, Long, Long>> hostMemoryMap) {
        // Create a copy of the base array and adjust only the two affected hosts
        double[] adjustedMetrics = new double[baseMetricsArray.length];
        System.arraycopy(baseMetricsArray, 0, adjustedMetrics, 0, baseMetricsArray.length);

        long destHostId = destHost.getId();
        long vmHostId = vm.getHostId();

        // Adjust source host (remove VM resources)
        Integer sourceIndex = hostIdToIndexMap.get(vmHostId);
        if (sourceIndex != null && sourceIndex < adjustedMetrics.length) {
            Map<Long, Ternary<Long, Long, Long>> sourceMetricsMap = getClusterDrsMetric(clusterId).equals("cpu") ? hostCpuMap : hostMemoryMap;
            Ternary<Long, Long, Long> sourceMetrics = sourceMetricsMap.get(vmHostId);
            if (sourceMetrics != null) {
                adjustedMetrics[sourceIndex] = getMetricValuePostMigration(clusterId, sourceMetrics, vmMetric, vmHostId, destHostId, vmHostId);
            }
        }

        // Adjust destination host (add VM resources)
        Integer destIndex = hostIdToIndexMap.get(destHostId);
        if (destIndex != null && destIndex < adjustedMetrics.length) {
            Map<Long, Ternary<Long, Long, Long>> destMetricsMap = getClusterDrsMetric(clusterId).equals("cpu") ? hostCpuMap : hostMemoryMap;
            Ternary<Long, Long, Long> destMetrics = destMetricsMap.get(destHostId);
            if (destMetrics != null) {
                adjustedMetrics[destIndex] = getMetricValuePostMigration(clusterId, destMetrics, vmMetric, destHostId, destHostId, vmHostId);
            }
        }

        return calculateImbalance(adjustedMetrics);
    }

    /**
     * Calculate imbalance from an array of metric values.
     * Imbalance is defined as standard deviation divided by mean.
     *
     * Uses reusable stateless calculator objects to avoid object creation overhead.
     * @param values array of metric values
     * @return calculated imbalance
     */
    private static double calculateImbalance(double[] values) {
        if (values == null || values.length == 0) {
            return 0.0;
        }

        double mean = MEAN_CALCULATOR.evaluate(values);
        if (mean == 0.0) {
            return 0.0; // Avoid division by zero
        }
        double stdDev = STDDEV_CALCULATOR.evaluate(values, mean);
        return stdDev / mean;
    }

    /**
     * Helper method to get VM metric based on cluster configuration.
     */
    static long getVmMetric(ServiceOffering serviceOffering, Long clusterId) throws ConfigurationException {
        String metric = getClusterDrsMetric(clusterId);
        switch (metric) {
            case "cpu":
                return (long) serviceOffering.getCpu() * serviceOffering.getSpeed();
            case "memory":
                return serviceOffering.getRamSize() * 1024L * 1024L;
            default:
                throw new ConfigurationException(
                        String.format("Invalid metric: %s for cluster: %d", metric, clusterId));
        }
    }

    /**
     * Helper method to calculate metrics from pre and post imbalance values.
     */
    default Ternary<Double, Double, Double> calculateMetricsFromImbalances(Double preImbalance, Double postImbalance) {
        // This needs more research to determine the cost and benefit of a migration
        // TODO: Cost should be a factor of the VM size and the host capacity
        // TODO: Benefit should be a factor of the VM size and the host capacity and the number of VMs on the host
        final double improvement = preImbalance - postImbalance;
        final double cost = 0.0;
        final double benefit = 1.0;
        return new Ternary<>(improvement, cost, benefit);
    }

    private Double getMetricValuePostMigration(Long clusterId, Ternary<Long, Long, Long> metrics, long vmMetric,
            long hostId, long destHostId, long vmHostId) {
        long used = metrics.first();
        long actualTotal = metrics.third() - metrics.second();
        long free = actualTotal - metrics.first();

        if (hostId == destHostId) {
            used += vmMetric;
            free -= vmMetric;
        } else if (hostId == vmHostId) {
            used -= vmMetric;
            free += vmMetric;
        }
        return getMetricValue(clusterId, used, free, actualTotal, null);
    }

    private static Double getImbalance(List<Double> metricList) {
        if (CollectionUtils.isEmpty(metricList)) {
            return 0.0;
        }
        // Convert List<Double> to double[] once, avoiding repeated conversions
        double[] values = new double[metricList.size()];
        int index = 0;
        for (Double value : metricList) {
            if (value != null) {
                values[index++] = value;
            }
        }

        // Trim array if some values were null
        if (index < values.length) {
            double[] trimmed = new double[index];
            System.arraycopy(values, 0, trimmed, 0, index);
            values = trimmed;
        }

        return calculateImbalance(values);
    }

    static String getClusterDrsMetric(long clusterId) {
        return ClusterDrsMetric.valueIn(clusterId);
    }

    static Double getMetricValue(long clusterId, long used, long free, long total, Float skipThreshold) {
        boolean useRatio = getDrsMetricUseRatio(clusterId);
        switch (getDrsMetricType(clusterId)) {
            case "free":
                if (skipThreshold != null && free < skipThreshold * total) return null;
                if (useRatio) {
                    return (double) free / total;
                } else {
                    return (double) free;
                }
            case "used":
                if (skipThreshold != null && used > skipThreshold * total) return null;
                if (useRatio) {
                    return (double) used / total;
                } else {
                    return (double) used;
                }
        }
        return null;
    }

    static boolean getDrsMetricUseRatio(long clusterId) {
        return ClusterDrsMetricUseRatio.valueIn(clusterId);
    }

    static String getDrsMetricType(long clusterId) {
        return ClusterDrsMetricType.valueIn(clusterId);
    }

    /**
     * The cluster imbalance is defined as the percentage deviation from the mean
     * for a configured metric of the cluster. The standard deviation is used as a
     * mathematical tool to normalize the metric data for all the resource and the
     * percentage deviation provides an easy tool to compare a cluster’s current
     * state against the defined imbalance threshold. Because this is essentially a
     * percentage, the value is a number between 0.0 and 1.0.
     * Cluster Imbalance, Ic = σc / mavg , where σc is the standard deviation and
     * mavg is the mean metric value for the cluster.
     */
    static Double getClusterImbalance(Long clusterId, List<Ternary<Long, Long, Long>> cpuList,
            List<Ternary<Long, Long, Long>> memoryList, Float skipThreshold) throws ConfigurationException {
        String metric = getClusterDrsMetric(clusterId);
        List<Double> list;
        switch (metric) {
            case "cpu":
                list = getMetricList(clusterId, cpuList, skipThreshold);
                break;
            case "memory":
                list = getMetricList(clusterId, memoryList, skipThreshold);
                break;
            default:
                throw new ConfigurationException(
                        String.format("Invalid metric: %s for cluster: %d", metric, clusterId));
        }
        return getImbalance(list);
    }

    static List<Double> getMetricList(Long clusterId, List<Ternary<Long, Long, Long>> hostMetricsList,
            Float skipThreshold) {
        List<Double> list = new ArrayList<>();
        for (Ternary<Long, Long, Long> ternary : hostMetricsList) {
            long used = ternary.first();
            long actualTotal = ternary.third() - ternary.second();
            long free = actualTotal - ternary.first();
            Double metricValue = getMetricValue(clusterId, used, free, actualTotal, skipThreshold);
            if (metricValue != null) {
                list.add(metricValue);
            }
        }
        return list;
    }
}
