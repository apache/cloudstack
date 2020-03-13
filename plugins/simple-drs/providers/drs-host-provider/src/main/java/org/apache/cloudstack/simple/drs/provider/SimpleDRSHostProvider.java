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
package org.apache.cloudstack.simple.drs.provider;

import com.cloud.exception.ManagementServerException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.org.Cluster;
import com.cloud.resource.ResourceManager;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmService;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.command.admin.host.ListHostsCmd;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.framework.simple.drs.DRSProvider;
import org.apache.cloudstack.query.QueryService;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.springframework.util.CollectionUtils;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;

public class SimpleDRSHostProvider extends AdapterBase implements DRSProvider {

    @Inject
    private HostDao hostDao;
    @Inject
    private VMInstanceDao vmInstanceDao;
    @Inject
    private UserVmService userVmService;
    @Inject
    private ResourceManager resourceManager;
    @Inject
    private QueryService queryService;

    private static final String PROVIDER_NAME = "host-vm";

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public double calculateClusterImbalance(long clusterId) {
        Cluster cluster = resourceManager.getCluster(clusterId);
        if (cluster == null) {
            throw new CloudRuntimeException("Could not find a cluster of hosts with ID = " + clusterId);
        }
        if (cluster.getHypervisorType() != Hypervisor.HypervisorType.KVM) {
            throw new CloudRuntimeException("The DRS host provider only supports KVM clusters of hosts");
        }
        ListHostsCmd cmd = new ListHostsCmd();
        cmd.setClusterId(clusterId);
        cmd.setType(Host.Type.Routing.name());
        cmd.setHypervisor(Hypervisor.HypervisorType.KVM);

        List<HostResponse> hostResponses = queryService.searchForServers(cmd).getResponses();
        if (CollectionUtils.isEmpty(hostResponses)) {
            throw new CloudRuntimeException("Could not find any hosts in the cluster with ID = " + clusterId);
        }
        List<HostResponse> resources = filterHosts(hostResponses);
        double[] hostsCpuAllocations = getNormalizedMetricsListFromHosts(hostResponses, cluster.getUuid());
        double stdDev = calculateClusterStandardDeviation(hostsCpuAllocations);
        double meanAvg = calculateClusterMean(hostsCpuAllocations);
        return stdDev / meanAvg;
    }

    private List<HostResponse> filterHosts(List<HostResponse> hostResponses) {
        return null;
    }

    protected double[] getNormalizedMetricsListFromHosts(List<HostResponse> hostResponses, String clusterUuid) {
        if (CollectionUtils.isEmpty(hostResponses)) {
            return null;
        }
        return hostResponses.stream()
                .filter(
                        x -> x.getRemoved() == null && x.getClusterId().equals(clusterUuid) &&
                                x.getHostType() == Host.Type.Routing && x.getHypervisor() == Hypervisor.HypervisorType.KVM)
                .mapToDouble(
                        x -> Double.parseDouble(x.getCpuAllocated().replaceAll("%", "")) / 100)
                .toArray();
    }

    private double calculateClusterStandardDeviation(double[] values) {
        StandardDeviation standardDeviation = new StandardDeviation();
        return standardDeviation.evaluate(values);
    }

    private double calculateClusterMean(double[] values) {
        Mean mean = new Mean();
        return mean.evaluate(values);
    }

    @Override
    public void performWorkloadRebalance(long clusterId, long workloadId, long destinationId) {
        VMInstanceVO workload = vmInstanceDao.findById(workloadId);
        if (workload == null) {
            return;
        }
        HostVO sourceHost = hostDao.findById(workload.getHostId());
        HostVO destHost = hostDao.findById(destinationId);
        try {
            VirtualMachine vm = userVmService.migrateVirtualMachine(workloadId, destHost);
        } catch (ResourceUnavailableException | ManagementServerException | VirtualMachineMigrationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<String> findPossibleRebalancingPlans(long clusterId) {
        return null;
    }
}
