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
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.Adapter;
import com.cloud.vm.VirtualMachine;
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
    boolean needsDrs(long clusterId, List<Ternary<Long, Long, Long>> cpuList,
            List<Ternary<Long, Long, Long>> memoryList) throws ConfigurationException;


    /**
     * Determines the metrics for a given virtual machine and destination host in a DRS cluster.
     *
     * @param clusterId
     *         the ID of the cluster to check
     * @param vm
     *         the virtual machine to check
     * @param serviceOffering
     *         the service offering for the virtual machine
     * @param destHost
     *         the destination host for the virtual machine
     * @param hostCpuMap
     *         a map of host IDs to the Ternary of used, reserved and total CPU on each host
     * @param hostMemoryMap
     *         a map of host IDs to the Ternary of used, reserved and total memory on each host
     * @param requiresStorageMotion
     *         whether storage motion is required for the virtual machine
     *
     * @return a ternary containing improvement, cost, benefit
     */
    Ternary<Double, Double, Double> getMetrics(long clusterId, VirtualMachine vm, ServiceOffering serviceOffering,
            Host destHost, Map<Long, Ternary<Long, Long, Long>> hostCpuMap,
            Map<Long, Ternary<Long, Long, Long>> hostMemoryMap,
            Boolean requiresStorageMotion) throws ConfigurationException;

    /**
     * Calculates the imbalance of the cluster after a virtual machine migration.
     *
     * @param serviceOffering
     *         the service offering for the virtual machine
     * @param vm
     *         the virtual machine being migrated
     * @param destHost
     *         the destination host for the virtual machine
     * @param hostCpuMap
     *         a map of host IDs to the Ternary of used, reserved and total CPU on each host
     * @param hostMemoryMap
     *         a map of host IDs to the Ternary of used, reserved and total memory on each host
     *
     * @return a pair containing the CPU and memory imbalance of the cluster after the migration
     */
    default Double getImbalancePostMigration(ServiceOffering serviceOffering, VirtualMachine vm,
            Host destHost, Map<Long, Ternary<Long, Long, Long>> hostCpuMap,
            Map<Long, Ternary<Long, Long, Long>> hostMemoryMap) throws ConfigurationException {
        Pair<Long, Map<Long, Ternary<Long, Long, Long>>> pair = getHostMetricsMapAndType(destHost.getClusterId(), serviceOffering, hostCpuMap, hostMemoryMap);
        long vmMetric = pair.first();
        Map<Long, Ternary<Long, Long, Long>> hostMetricsMap = pair.second();

        List<Double> list = new ArrayList<>();
        for (Long hostId : hostMetricsMap.keySet()) {
            list.add(getMetricValuePostMigration(destHost.getClusterId(), hostMetricsMap.get(hostId), vmMetric, hostId, destHost.getId(), vm.getHostId()));
        }
        return getImbalance(list);
    }

    private Pair<Long, Map<Long, Ternary<Long, Long, Long>>> getHostMetricsMapAndType(Long clusterId,
            ServiceOffering serviceOffering, Map<Long, Ternary<Long, Long, Long>> hostCpuMap,
            Map<Long, Ternary<Long, Long, Long>> hostMemoryMap) throws ConfigurationException {
        String metric = getClusterDrsMetric(clusterId);
        Pair<Long, Map<Long, Ternary<Long, Long, Long>>> pair;
        switch (metric) {
            case "cpu":
                pair = new Pair<>((long) serviceOffering.getCpu() * serviceOffering.getSpeed(), hostCpuMap);
                break;
            case "memory":
                pair = new Pair<>(serviceOffering.getRamSize() * 1024L * 1024L, hostMemoryMap);
                break;
            default:
                throw new ConfigurationException(
                        String.format("Invalid metric: %s for cluster: %d", metric, clusterId));
        }
        return pair;
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
        Double clusterMeanMetric = getClusterMeanMetric(metricList);
        Double clusterStandardDeviation = getClusterStandardDeviation(metricList, clusterMeanMetric);
        return clusterStandardDeviation / clusterMeanMetric;
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

    /**
     * Mean is the average of a collection or set of metrics. In context of a DRS
     * cluster, the cluster metrics defined as the average metrics value for some
     * metric (such as CPU, memory etc.) for every resource such as host.
     * Cluster Mean Metric, mavg = (∑mi) / N, where mi is a measurable metric for a
     * resource ‘i’ in a cluster with total N number of resources.
     */
    static Double getClusterMeanMetric(List<Double> metricList) {
        return new Mean().evaluate(metricList.stream().mapToDouble(i -> i).toArray());
    }

    /**
     * Standard deviation is defined as the square root of the absolute squared sum
     * of difference of a metric from its mean for every resource divided by the
     * total number of resources. In context of the DRS, the cluster standard
     * deviation is the standard deviation based on a metric of resources in a
     * cluster such as for the allocation or utilisation CPU/memory metric of hosts
     * in a cluster.
     * Cluster Standard Deviation, σc = sqrt((∑∣mi−mavg∣^2) / N), where mavg is the
     * mean metric value and mi is a measurable metric for some resource ‘i’ in the
     * cluster with total N number of resources.
     */
    static Double getClusterStandardDeviation(List<Double> metricList, Double mean) {
        if (mean != null) {
            return new StandardDeviation(false).evaluate(metricList.stream().mapToDouble(i -> i).toArray(), mean);
        } else {
            return new StandardDeviation(false).evaluate(metricList.stream().mapToDouble(i -> i).toArray());
        }
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
