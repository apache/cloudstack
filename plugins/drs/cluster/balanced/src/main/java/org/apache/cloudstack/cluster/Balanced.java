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
import com.cloud.utils.Ternary;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.VirtualMachine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.cloudstack.cluster.ClusterDrsService.ClusterDrsImbalanceThreshold;

public class Balanced extends AdapterBase implements ClusterDrsAlgorithm {

    private static final Logger logger = LogManager.getLogger(Balanced.class);

    @Override
    public boolean needsDrs(long clusterId, List<Ternary<Long, Long, Long>> cpuList,
            List<Ternary<Long, Long, Long>> memoryList) throws ConfigurationException {
        double threshold = getThreshold(clusterId);
        Double imbalance = ClusterDrsAlgorithm.getClusterImbalance(clusterId, cpuList, memoryList, null);
        String drsMetric = ClusterDrsAlgorithm.getClusterDrsMetric(clusterId);
        String metricType = ClusterDrsAlgorithm.getDrsMetricType(clusterId);
        Boolean useRatio = ClusterDrsAlgorithm.getDrsMetricUseRatio(clusterId);
        if (imbalance > threshold) {
            logger.debug(String.format("Cluster %d needs DRS. Imbalance: %s Threshold: %s Algorithm: %s DRS metric: %s Metric Type: %s Use ratio: %s",
                    clusterId, imbalance, threshold, getName(), drsMetric, metricType, useRatio));
            return true;
        } else {
            logger.debug(String.format("Cluster %d does not need DRS. Imbalance: %s Threshold: %s Algorithm: %s DRS metric: %s Metric Type: %s Use ratio: %s",
                    clusterId, imbalance, threshold, getName(), drsMetric, metricType, useRatio));
            return false;
        }
    }

    private double getThreshold(long clusterId) {
        return 1.0 - ClusterDrsImbalanceThreshold.valueIn(clusterId);
    }

    @Override
    public String getName() {
        return "balanced";
    }

    @Override
    public Ternary<Double, Double, Double> getMetrics(long clusterId, VirtualMachine vm,
            ServiceOffering serviceOffering, Host destHost,
            Map<Long, Ternary<Long, Long, Long>> hostCpuMap, Map<Long, Ternary<Long, Long, Long>> hostMemoryMap,
            Boolean requiresStorageMotion) throws ConfigurationException {
        Double preImbalance = ClusterDrsAlgorithm.getClusterImbalance(clusterId, new ArrayList<>(hostCpuMap.values()), new ArrayList<>(hostMemoryMap.values()), null);
        Double postImbalance = getImbalancePostMigration(serviceOffering, vm, destHost, hostCpuMap, hostMemoryMap);

        logger.debug(String.format("Cluster %d pre-imbalance: %s post-imbalance: %s Algorithm: %s VM: %s srcHost: %d destHost: %s",
                clusterId, preImbalance, postImbalance, getName(), vm.getUuid(), vm.getHostId(), destHost.getUuid()));

        // This needs more research to determine the cost and benefit of a migration
        // TODO: Cost should be a factor of the VM size and the host capacity
        // TODO: Benefit should be a factor of the VM size and the host capacity and the number of VMs on the host
        final double improvement = preImbalance - postImbalance;
        final double cost = 0.0;
        final double benefit = 1.0;
        return new Ternary<>(improvement, cost, benefit);
    }
}
