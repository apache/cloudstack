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
import org.apache.cloudstack.framework.config.ConfigKey;

import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.cloudstack.cluster.ClusterDrsService.ClusterDrsImbalanceThreshold;
import static org.apache.cloudstack.cluster.ClusterDrsService.ClusterDrsMetricType;

public class Condensed extends AdapterBase implements ClusterDrsAlgorithm {

    ConfigKey<Float> ClusterDrsImbalanceSkipThreshold = new ConfigKey<>(Float.class,
            "drs.imbalance.condensed.skip.threshold", ConfigKey.CATEGORY_ADVANCED, "0.95",
            "Threshold to ignore the metric for a host while calculating the imbalance to decide " +
                    "whether DRS is required for a cluster.This is to avoid cases when the calculated imbalance" +
                    " gets skewed due to a single host having a very high/low metric  value resulting in imbalance" +
                    " being higher than 1. If " + ClusterDrsMetricType.key() + " is 'free', set a lower value and if it is 'used' " +
                    "set a higher value. The value should be between 0.0 and 1.0",
            true, ConfigKey.Scope.Cluster, null, "DRS imbalance", null, null, null);

    @Override
    public String getName() {
        return "condensed";
    }

    @Override
    public boolean needsDrs(long clusterId, List<Ternary<Long, Long, Long>> cpuList,
            List<Ternary<Long, Long, Long>> memoryList) throws ConfigurationException {
        double threshold = getThreshold(clusterId);
        Double imbalance = getClusterImbalance(clusterId, cpuList, memoryList, ClusterDrsImbalanceSkipThreshold.valueIn(clusterId));
        return imbalance < threshold;
    }

    private double getThreshold(long clusterId) {
        return ClusterDrsImbalanceThreshold.valueIn(clusterId);
    }

    @Override
    public Ternary<Double, Double, Double> getMetrics(long clusterId, VirtualMachine vm,
            ServiceOffering serviceOffering, Host destHost,
            Map<Long, Ternary<Long, Long, Long>> hostCpuMap, Map<Long, Ternary<Long, Long, Long>> hostMemoryMap,
            Boolean requiresStorageMotion) throws ConfigurationException {
        Double preImbalance = getClusterImbalance(clusterId, new ArrayList<>(hostCpuMap.values()), new ArrayList<>(hostMemoryMap.values()), null);
        Double postImbalance = getImbalancePostMigration(serviceOffering, vm, destHost, hostCpuMap, hostMemoryMap);

        // This needs more research to determine the cost and benefit of a migration
        // TODO: Cost should be a factor of the VM size and the host capacity
        // TODO: Benefit should be a factor of the VM size and the host capacity and the number of VMs on the host
        final double improvement = postImbalance - preImbalance;
        final double cost = 0;
        final double benefit = 1;
        return new Ternary<>(improvement, cost, benefit);
    }
}
