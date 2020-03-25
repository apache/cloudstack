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
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.org.Cluster;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmDetailVO;
import com.cloud.vm.UserVmService;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.command.admin.host.ListHostsCmd;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.framework.simple.drs.SimpleDRSProvider;
import org.apache.cloudstack.framework.simple.drs.SimpleDRSProviderBase;
import org.apache.cloudstack.query.QueryService;
import org.apache.cloudstack.simple.drs.SimpleDRSManager;
import org.apache.cloudstack.simple.drs.SimpleDRSResource;
import org.apache.cloudstack.simple.drs.SimpleDRSWorkload;
import org.apache.commons.lang.BooleanUtils;
import org.apache.log4j.Logger;
import org.springframework.util.CollectionUtils;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;

public class SimpleDRSHostProvider extends SimpleDRSProviderBase implements SimpleDRSProvider {

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
    @Inject
    private HostDetailsDao hostDetailsDao;
    @Inject
    private UserVmDetailsDao userVmDetailsDao;
    @Inject
    private ServiceOfferingDao serviceOfferingDao;

    private static final String PROVIDER_NAME = "host-vm";
    public static final Logger LOG = Logger.getLogger(SimpleDRSHostProvider.class);

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    private Cluster getHostCluster(long clusterId) {
        Cluster cluster = resourceManager.getCluster(clusterId);
        if (cluster == null) {
            throw new CloudRuntimeException("Could not find a cluster of hosts with ID = " + clusterId);
        }
        if (cluster.getHypervisorType() != Hypervisor.HypervisorType.KVM) {
            throw new CloudRuntimeException("The DRS provider " + PROVIDER_NAME + " only supports KVM clusters of hosts");
        }
        return cluster;
    }

    /**
     * Retrieve the hosts within a cluster with the available metrics
     */
    private List<HostResponse> getClusterHostsWithMetrics(long clusterId) {
        ListHostsCmd cmd = new ListHostsCmd();
        cmd.setClusterId(clusterId);
        cmd.setType(Host.Type.Routing.name());
        cmd.setHypervisor(Hypervisor.HypervisorType.KVM);
        List<HostResponse> hostResponses = queryService.searchForServers(cmd).getResponses();
        if (CollectionUtils.isEmpty(hostResponses)) {
            LOG.debug("Could not find any hosts in the cluster with ID = " + clusterId);
        }
        return hostResponses;
    }

    /**
     * Retrieve the VMs within a host with the available metrics
     */
    private List<SimpleDRSWorkload> getVMsInHost(long hostId) {
        List<SimpleDRSWorkload> workloads = new LinkedList<>();
        List<VMInstanceVO> vms = vmInstanceDao.listByHostId(hostId);
        for (SimpleDRSWorkload vm : vms) {
            UserVmDetailVO detail = userVmDetailsDao.findDetail(vm.getId(), GRANULAR_DRS_DETAIL_NAME);
            if (detail != null && BooleanUtils.toBoolean(detail.getValue())) {
                LOG.debug("Found granular VM detail to exclude VM with ID = " + vm.getUuid() + " from DRS");
                continue;
            }
            long serviceOfferingId = ((VMInstanceVO) vm).getServiceOfferingId();
            ServiceOfferingVO serviceOffering = serviceOfferingDao.findById(vm.getId(), serviceOfferingId);
            Integer cpus = serviceOffering.getCpu();
            Integer speedMhz = serviceOffering.getSpeed();
            vm.setDRSRebalanceMetric((float) cpus * speedMhz / 1000);
            workloads.add(vm);
        }
        return workloads;
    }

    @Override
    protected double[] generateClusterMetricValues(long clusterId) {
        Cluster cluster = getHostCluster(clusterId);
        List<HostResponse> hostResponses = getClusterHostsWithMetrics(cluster.getId());
        return getNormalizedMetricsListFromHosts(hostResponses);
    }

    double[] getNormalizedMetricsListFromHosts(List<HostResponse> hostResponses) {
        if (CollectionUtils.isEmpty(hostResponses)) {
            return null;
        }
        return hostResponses.stream()
                .mapToDouble(
                        x -> Double.parseDouble(x.getCpuAllocated().replaceAll("%", "")) / 100)
                .toArray();
    }

    @Override
    public boolean performWorkloadRebalance(SimpleDRSWorkload workload, SimpleDRSResource destination) {
        VirtualMachine vm = (VirtualMachine) workload;
        if (vm.getHypervisorType() != Hypervisor.HypervisorType.KVM) {
            throw new CloudRuntimeException("The DRS provider " + PROVIDER_NAME + " only supports KVM clusters of hosts");
        }
        Host destHost = (Host) destination;
        try {
            VirtualMachine vmMigrated = userVmService.migrateVirtualMachine(workload.getId(), destHost);
        } catch (ResourceUnavailableException | ManagementServerException | VirtualMachineMigrationException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public List<String> findPossibleRebalancingPlans(long clusterId) {
        return null;
    }

    @Override
    public List<SimpleDRSResource> findResourcesToBalance(long clusterId) {
        Cluster cluster = getHostCluster(clusterId);
        List<HostResponse> hostResponses = getClusterHostsWithMetrics(cluster.getId());
        Boolean drsEnabled = SimpleDRSManager.SimpleDRSAutomaticEnable.valueIn(cluster.getId());
        List<SimpleDRSResource> resources = new LinkedList<>();
        if (BooleanUtils.isTrue(drsEnabled)) {
            for (HostResponse hostWithMetrics : hostResponses) {
                SimpleDRSResource host = hostDao.findByUuid(hostWithMetrics.getId());
                DetailVO granularDrsEnabled = hostDetailsDao.findDetail(host.getId(), GRANULAR_DRS_DETAIL_NAME);
                if (granularDrsEnabled != null && BooleanUtils.toBoolean(granularDrsEnabled.getValue())) {
                    LOG.debug("Found granular host detail to disable DRS on host " + hostWithMetrics.getId() + " (" + hostWithMetrics.getName() + " )" +
                            ", skipping host from the resources to balance");
                    continue;
                }
                host.setDRSRebalanceMetric(Double.parseDouble(hostWithMetrics.getCpuAllocated()) / 100);
                resources.add(host);
            }
        }
        return resources;
    }

    @Override
    public List<SimpleDRSWorkload> findWorkloadsInResource(long clusterId, long resourceId) {
        return getVMsInHost(resourceId);
    }
}
