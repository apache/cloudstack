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

import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.cloudstack.cluster.ClusterDrsService.ClusterDrsImbalanceThreshold;

public class Balanced extends AdapterBase implements ClusterDrsAlgorithm {

    @Override
    public String getName() {
        return "balanced";
    }

    @Override
    public boolean needsDrs(long clusterId, List<Ternary<Long, Long, Long>> cpuList,
            List<Ternary<Long, Long, Long>> memoryList) throws ConfigurationException {
        double threshold = getThreshold(clusterId);
        Double imbalance = getClusterImbalance(clusterId, cpuList, memoryList, null);
        return imbalance > threshold;
    }

    private double getThreshold(long clusterId) {
        return 1.0 - ClusterDrsImbalanceThreshold.valueIn(clusterId);
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
        final double improvement = preImbalance - postImbalance;
        final double cost = 0.0;
        final double benefit = 1.0;
        return new Ternary<>(improvement, cost, benefit);
    }
}
