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

import com.cloud.api.query.dao.HostJoinDao;
import com.cloud.host.Host;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.VirtualMachine;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.cloudstack.cluster.ClusterDrsService.ClusterDrsMetric;
import static org.apache.cloudstack.cluster.ClusterDrsService.ClusterDrsThreshold;

public class Condensed extends AdapterBase implements ClusterDrsAlgorithm {

    @Inject
    ServiceOfferingDao serviceOfferingDao;

    @Inject
    HostJoinDao hostJoinDao;

    @Override
    public String getName() {
        return "condensed";
    }

    @Override
    public boolean needsDrs(long clusterId, List<Long> cpuList, List<Long> memoryList) throws ConfigurationException {
        Double cpuImbalance = getClusterImbalance(cpuList);
        Double memoryImbalance = getClusterImbalance(memoryList);
        Double threshold = ClusterDrsThreshold.valueIn(clusterId);
        String metric = ClusterDrsMetric.valueIn(clusterId);
        switch (metric) {
            case "cpu":
                return cpuImbalance < threshold;
            case "memory":
                return memoryImbalance < threshold;
            case "both":
                return cpuImbalance < threshold && memoryImbalance < threshold;
            case "either":
                return cpuImbalance < threshold || memoryImbalance < threshold;
            default:
                throw new ConfigurationException(String.format("Invalid metric: %s for cluster: %d", metric, clusterId));
        }
    }

    @Override
    public Ternary<Double, Double, Double> getMetrics(long clusterId, VirtualMachine vm, Host destHost, Map<Long, Long> hostCpuUsedMap, Map<Long, Long> hostMemoryUsedMap, Boolean requiresStorageMotion) {
        List<Long> cpuList = new ArrayList<>();
        List<Long> memoryList = new ArrayList<>();
        List<Long> postCpuList = new ArrayList<>();
        List<Long> postMemoryList = new ArrayList<>();
        ServiceOffering serviceOffering = serviceOfferingDao.findByIdIncludingRemoved(vm.getId(), vm.getServiceOfferingId());

        for (Long hostId : hostCpuUsedMap.keySet()) {
            long cpu = hostCpuUsedMap.get(hostId);
            long memory = hostMemoryUsedMap.get(hostId);
            if (hostId == destHost.getId()) {
                if (memory + serviceOffering.getRamSize() > destHost.getTotalMemory()) {
                    return new Ternary<>(-1.0, 1.0, -1.0);
                }
                // TODO: Revisit this check for overcommitting of resources
                if (cpu + serviceOffering.getCpu() > destHost.getCpus()) {
                    return new Ternary<>(-1.0, 1.0, -1.0);
                }
                postCpuList.add(cpu + serviceOffering.getCpu());
                postMemoryList.add(memory + serviceOffering.getRamSize());
            } else if (hostId.equals(vm.getHostId())) {
                postCpuList.add(cpu - serviceOffering.getCpu());
                postMemoryList.add(memory - serviceOffering.getRamSize());
            } else {
                postCpuList.add(cpu);
                postMemoryList.add(memory);
            }
        }

        Double preCpuImbalance = getClusterImbalance(cpuList);
        Double preMemoryImbalance = getClusterImbalance(memoryList);
        Double postCpuImbalance = getClusterImbalance(postCpuList);
        Double postMemoryImbalance = getClusterImbalance(postMemoryList);

        // TODO: Cost should also include the cost of storage motion
        // TODO: Cost should also consider dirt memory pages
        double cost = serviceOffering.getRamSize();
        double benefit = (postMemoryImbalance - preMemoryImbalance) * destHost.getTotalMemory();

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
