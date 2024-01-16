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
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.VirtualMachine;

import javax.naming.ConfigurationException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.cloudstack.cluster.ClusterDrsService.ClusterDrsImbalanceThreshold;
import static org.apache.cloudstack.cluster.ClusterDrsService.ClusterDrsMetric;

public class Condensed extends AdapterBase implements ClusterDrsAlgorithm {

    @Override
    public String getName() {
        return "condensed";
    }

    @Override
    public boolean needsDrs(long clusterId, List<Pair<Long, Long>> cpuList, List<Pair<Long, Long>> memoryList) throws ConfigurationException {
        double threshold = getThreshold(clusterId);
        String metric = ClusterDrsMetric.valueIn(clusterId);
        switch (metric) {
            case "cpu":
                List<Double> cpuRatioList = cpuList.stream().map(pair -> (double) pair.first() / pair.second()).collect(Collectors.toList());
                Double cpuImbalance = getClusterImbalance(cpuRatioList);
                return cpuImbalance < threshold;
            case "memory":
                List<Double> memoryRatioList = memoryList.stream().map(pair -> (double) pair.first() / pair.second()).collect(Collectors.toList());
                Double memoryImbalance = getClusterImbalance(memoryRatioList);
                return memoryImbalance < threshold;
            default:
                throw new ConfigurationException(
                        String.format("Invalid metric: %s for cluster: %d", metric, clusterId));
        }
    }

    private double getThreshold(long clusterId) throws ConfigurationException {
        return ClusterDrsImbalanceThreshold.valueIn(clusterId);
    }

    @Override
    public Ternary<Double, Double, Double> getMetrics(long clusterId, VirtualMachine vm,
                                                      ServiceOffering serviceOffering, Host destHost,
                                                      Map<Long, Pair<Long, Long>> hostCpuUsedMap, Map<Long, Pair<Long, Long>> hostMemoryUsedMap,
                                                      Boolean requiresStorageMotion) {
        List<Double> cpuList = hostCpuUsedMap.values().stream().map(pair -> (double) pair.first() / pair.second()).collect(Collectors.toList());
        Double preCpuImbalance = getClusterImbalance(cpuList);
        List<Double> memoryList = hostMemoryUsedMap.values().stream().map(pair -> (double) pair.first() / pair.second()).collect(Collectors.toList());
        Double preMemoryImbalance = getClusterImbalance(memoryList);

        Pair<Double, Double> imbalancePair = getImbalancePostMigration(serviceOffering, vm, destHost, hostCpuUsedMap,
                hostMemoryUsedMap);
        Double postCpuImbalance = imbalancePair.first();
        Double postMemoryImbalance = imbalancePair.second();

        // This needs more research to determine the cost and benefit of a migration
        // TODO: Cost should be a factor of the VM size and the host capacity
        // TODO: Benefit should be a factor of the VM size and the host capacity and the number of VMs on the host
        double cost = 0;
        double benefit = 1;

        String metric = ClusterDrsMetric.valueIn(clusterId);
        double improvement;
        switch (metric) {
            case "cpu":
                improvement = postCpuImbalance - preCpuImbalance;
                break;
            case "memory":
                improvement = postMemoryImbalance - preMemoryImbalance;
                break;
            default:
                improvement = postCpuImbalance + postMemoryImbalance - preCpuImbalance - preMemoryImbalance;
        }
        return new Ternary<>(improvement, cost, benefit);
    }
}
