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

public interface ClusterDrsAlgorithm extends Adapter {

    /**
     * Determines whether a DRS operation is needed for a given cluster and host-VM
     * mapping.
     *
     * @param clusterId
     *         the ID of the cluster to check
     * @param cpuList
     *         a list of CPU allocated values for each host in the cluster
     * @param memoryList
     *         a list of memory allocated values for each host in the cluster
     *
     * @return true if a DRS operation is needed, false otherwise
     *
     * @throws ConfigurationException
     *         if there is an error in the configuration
     */
    boolean needsDrs(long clusterId, List<Long> cpuList, List<Long> memoryList) throws ConfigurationException;


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
     * @param hostCpuUsedMap
     *         a map of host IDs to the amount of CPU used on each host
     * @param hostMemoryUsedMap
     *         a map of host IDs to the amount of memory used on each host
     * @param requiresStorageMotion
     *         whether storage motion is required for the virtual machine
     *
     * @return a ternary containing improvement, cost, benefit
     */
    Ternary<Double, Double, Double> getMetrics(long clusterId, VirtualMachine vm, ServiceOffering serviceOffering,
                                               Host destHost, Map<Long, Long> hostCpuUsedMap,
                                               Map<Long, Long> hostMemoryUsedMap, Boolean requiresStorageMotion);

    /**
     * Calculates the imbalance of the cluster after a virtual machine migration.
     *
     * @param serviceOffering
     *         the service offering for the virtual machine
     * @param vm
     *         the virtual machine being migrated
     * @param destHost
     *         the destination host for the virtual machine
     * @param hostCpuUsedMap
     *         a map of host IDs to the amount of CPU used on each host
     * @param hostMemoryUsedMap
     *         a map of host IDs to the amount of memory used on each host
     *
     * @return a pair containing the CPU and memory imbalance of the cluster after the migration
     */
    default Pair<Double, Double> getImbalancePostMigration(ServiceOffering serviceOffering, VirtualMachine vm,
                                                           Host destHost, Map<Long, Long> hostCpuUsedMap,
                                                           Map<Long, Long> hostMemoryUsedMap) {
        List<Long> postCpuList = new ArrayList<>();
        List<Long> postMemoryList = new ArrayList<>();
        final int vmCpu = serviceOffering.getCpu() * serviceOffering.getSpeed();
        final long vmRam = serviceOffering.getRamSize() * 1024L * 1024L;

        for (Long hostId : hostCpuUsedMap.keySet()) {
            long cpu = hostCpuUsedMap.get(hostId);
            long memory = hostMemoryUsedMap.get(hostId);
            if (hostId == destHost.getId()) {
                postCpuList.add(cpu + vmCpu);
                postMemoryList.add(memory + vmRam);
            } else if (hostId.equals(vm.getHostId())) {
                postCpuList.add(cpu - vmCpu);
                postMemoryList.add(memory - vmRam);
            } else {
                postCpuList.add(cpu);
                postMemoryList.add(memory);
            }
        }
        return new Pair<>(getClusterImbalance(postCpuList), getClusterImbalance(postMemoryList));
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
    default Double getClusterImbalance(List<Long> metricList) {
        Double clusterMeanMetric = getClusterMeanMetric(metricList);
        Double clusterStandardDeviation = getClusterStandardDeviation(metricList, clusterMeanMetric);
        return clusterStandardDeviation / clusterMeanMetric;
    }

    /**
     * Mean is the average of a collection or set of metrics. In context of a DRS
     * cluster, the cluster metrics defined as the average metrics value for some
     * metric (such as CPU, memory etc.) for every resource such as host.
     * Cluster Mean Metric, mavg = (∑mi) / N, where mi is a measurable metric for a
     * resource ‘i’ in a cluster with total N number of resources.
     */
    default Double getClusterMeanMetric(List<Long> metricList) {
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
    default Double getClusterStandardDeviation(List<Long> metricList, Double mean) {
        if (mean != null) {
            return new StandardDeviation(false).evaluate(metricList.stream().mapToDouble(i -> i).toArray(), mean);
        } else {
            return new StandardDeviation(false).evaluate(metricList.stream().mapToDouble(i -> i).toArray());
        }
    }
}
