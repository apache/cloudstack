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

package org.apache.cloudstack.metrics;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ListClustersMetricsCmd;
import org.apache.cloudstack.api.ListHostsMetricsCmd;
import org.apache.cloudstack.api.ListInfrastructureCmd;
import org.apache.cloudstack.api.ListStoragePoolsMetricsCmd;
import org.apache.cloudstack.api.ListVMsMetricsCmd;
import org.apache.cloudstack.api.ListVolumesMetricsCmd;
import org.apache.cloudstack.api.ListZonesMetricsCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.response.ClusterMetricsResponse;
import org.apache.cloudstack.response.HostMetricsResponse;
import org.apache.cloudstack.response.InfrastructureResponse;
import org.apache.cloudstack.response.StoragePoolMetricsResponse;
import org.apache.cloudstack.response.VmMetricsResponse;
import org.apache.cloudstack.response.VolumeMetricsResponse;
import org.apache.cloudstack.response.ZoneMetricsResponse;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.commons.beanutils.BeanUtils;

import com.cloud.alert.AlertManager;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.dao.HostJoinDao;
import com.cloud.api.query.vo.HostJoinVO;
import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityManager;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.capacity.dao.CapacityDaoImpl;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DeploymentClusterPlanner;
import com.cloud.host.Host;
import com.cloud.host.HostStats;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.org.Cluster;
import com.cloud.org.Grouping;
import com.cloud.org.Managed;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.component.ComponentLifecycleBase;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.VMInstanceDao;

public class MetricsServiceImpl extends ComponentLifecycleBase implements MetricsService {

    @Inject
    private DataCenterDao dataCenterDao;
    @Inject
    private HostPodDao podDao;
    @Inject
    private ClusterDao clusterDao;
    @Inject
    private HostDao hostDao;
    @Inject
    private HostJoinDao hostJoinDao;
    @Inject
    private PrimaryDataStoreDao storagePoolDao;
    @Inject
    private ImageStoreDao imageStoreDao;
    @Inject
    private VMInstanceDao vmInstanceDao;
    @Inject
    private DomainRouterDao domainRouterDao;
    @Inject
    private CapacityDao capacityDao;
    @Inject
    private AccountManager accountMgr;
    @Inject
    private ManagementServerHostDao managementServerHostDao;

    protected MetricsServiceImpl() {
        super();
    }

    private Double findRatioValue(final String value) {
        if (value != null) {
            return Double.valueOf(value);
        }
        return 1.0;
    }

    private void updateHostMetrics(final Metrics metrics, final HostJoinVO host) {
        metrics.incrTotalHosts();
        metrics.addCpuAllocated(host.getCpuReservedCapacity() + host.getCpuUsedCapacity());
        metrics.addMemoryAllocated(host.getMemReservedCapacity() + host.getMemUsedCapacity());
        final HostStats hostStats = ApiDBUtils.getHostStatistics(host.getId());
        if (hostStats != null) {
            metrics.addCpuUsedPercentage(hostStats.getCpuUtilization());
            metrics.addMemoryUsed((long) hostStats.getUsedMemory());
            metrics.setMaximumCpuUsage(hostStats.getCpuUtilization());
            metrics.setMaximumMemoryUsage((long) hostStats.getUsedMemory());
        }
    }

    @Override
    public InfrastructureResponse listInfrastructure() {
        final InfrastructureResponse response = new InfrastructureResponse();
        response.setZones(dataCenterDao.listAllZones().size());
        response.setPods(podDao.listAllPods(null).size());
        response.setClusters(clusterDao.listAllClusters(null).size());
        response.setHosts(hostDao.listByType(Host.Type.Routing).size());
        response.setStoragePools(storagePoolDao.listAll().size());
        response.setImageStores(imageStoreDao.listImageStores().size());
        response.setSystemvms(vmInstanceDao.listByTypes(VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm).size());
        response.setRouters(domainRouterDao.listAll().size());
        int cpuSockets = 0;
        for (final Host host : hostDao.listByType(Host.Type.Routing)) {
            if (host.getCpuSockets() != null) {
                cpuSockets += host.getCpuSockets();
            }
        }
        response.setCpuSockets(cpuSockets);
        response.setManagementServers(managementServerHostDao.listAll().size());
        return response;
    }

    @Override
    public List<VolumeMetricsResponse> listVolumeMetrics(List<VolumeResponse> volumeResponses) {
        final List<VolumeMetricsResponse> metricsResponses = new ArrayList<>();
        for (final VolumeResponse volumeResponse: volumeResponses) {
            VolumeMetricsResponse metricsResponse = new VolumeMetricsResponse();

            try {
                BeanUtils.copyProperties(metricsResponse, volumeResponse);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to generate volume metrics response");
            }

            metricsResponse.setDiskSizeGB(volumeResponse.getSize());
            metricsResponse.setDiskIopsTotal(volumeResponse.getDiskIORead(), volumeResponse.getDiskIOWrite());
            Account account = CallContext.current().getCallingAccount();
            if (accountMgr.isAdmin(account.getAccountId())) {
                metricsResponse.setStorageType(volumeResponse.getStorageType(), volumeResponse.getVolumeType());
            }
            metricsResponses.add(metricsResponse);
        }
        return metricsResponses;
    }

    @Override
    public List<VmMetricsResponse> listVmMetrics(List<UserVmResponse> vmResponses) {
        final List<VmMetricsResponse> metricsResponses = new ArrayList<>();
        for (final UserVmResponse vmResponse: vmResponses) {
            VmMetricsResponse metricsResponse = new VmMetricsResponse();

            try {
                BeanUtils.copyProperties(metricsResponse, vmResponse);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to generate vm metrics response");
            }

            metricsResponse.setIpAddress(vmResponse.getNics());
            metricsResponse.setCpuTotal(vmResponse.getCpuNumber(), vmResponse.getCpuSpeed());
            metricsResponse.setMemTotal(vmResponse.getMemory());
            metricsResponse.setNetworkRead(vmResponse.getNetworkKbsRead());
            metricsResponse.setNetworkWrite(vmResponse.getNetworkKbsWrite());
            metricsResponse.setDiskRead(vmResponse.getDiskKbsRead());
            metricsResponse.setDiskWrite(vmResponse.getDiskKbsWrite());
            metricsResponse.setDiskIopsTotal(vmResponse.getDiskIORead(), vmResponse.getDiskIOWrite());
            metricsResponses.add(metricsResponse);
        }
        return metricsResponses;
    }

    @Override
    public List<StoragePoolMetricsResponse> listStoragePoolMetrics(List<StoragePoolResponse> poolResponses) {
        final List<StoragePoolMetricsResponse> metricsResponses = new ArrayList<>();
        for (final StoragePoolResponse poolResponse: poolResponses) {
            StoragePoolMetricsResponse metricsResponse = new StoragePoolMetricsResponse();

            try {
                BeanUtils.copyProperties(metricsResponse, poolResponse);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to generate storagepool metrics response");
            }

            Long poolClusterId = null;
            final Cluster cluster = clusterDao.findByUuid(poolResponse.getClusterId());
            if (cluster != null) {
                poolClusterId = cluster.getId();
            }
            final Double storageThreshold = AlertManager.StorageCapacityThreshold.valueIn(poolClusterId);
            final Double storageDisableThreshold = CapacityManager.StorageCapacityDisableThreshold.valueIn(poolClusterId);

            metricsResponse.setDiskSizeUsedGB(poolResponse.getDiskSizeUsed());
            metricsResponse.setDiskSizeTotalGB(poolResponse.getDiskSizeTotal(), poolResponse.getOverProvisionFactor());
            metricsResponse.setDiskSizeAllocatedGB(poolResponse.getDiskSizeAllocated());
            metricsResponse.setDiskSizeUnallocatedGB(poolResponse.getDiskSizeTotal(), poolResponse.getDiskSizeAllocated(), poolResponse.getOverProvisionFactor());
            metricsResponse.setStorageUsedThreshold(poolResponse.getDiskSizeTotal(), poolResponse.getDiskSizeUsed(), poolResponse.getOverProvisionFactor(), storageThreshold);
            metricsResponse.setStorageUsedDisableThreshold(poolResponse.getDiskSizeTotal(), poolResponse.getDiskSizeUsed(), poolResponse.getOverProvisionFactor(), storageDisableThreshold);
            metricsResponse.setStorageAllocatedThreshold(poolResponse.getDiskSizeTotal(), poolResponse.getDiskSizeAllocated(), poolResponse.getOverProvisionFactor(), storageThreshold);
            metricsResponse.setStorageAllocatedDisableThreshold(poolResponse.getDiskSizeTotal(), poolResponse.getDiskSizeUsed(), poolResponse.getOverProvisionFactor(), storageDisableThreshold);
            metricsResponses.add(metricsResponse);
        }
        return metricsResponses;
    }

    @Override
    public List<HostMetricsResponse> listHostMetrics(List<HostResponse> hostResponses) {
        final List<HostMetricsResponse> metricsResponses = new ArrayList<>();
        for (final HostResponse hostResponse: hostResponses) {
            HostMetricsResponse metricsResponse = new HostMetricsResponse();

            try {
                BeanUtils.copyProperties(metricsResponse, hostResponse);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to generate host metrics response");
            }

            final Host host = hostDao.findByUuid(hostResponse.getId());
            if (host == null) {
                continue;
            }
            final Long hostId = host.getId();
            final Long clusterId = host.getClusterId();

            // Thresholds
            final Double cpuThreshold = AlertManager.CPUCapacityThreshold.valueIn(clusterId);
            final Double memoryThreshold = AlertManager.MemoryCapacityThreshold.valueIn(clusterId);
            final Float cpuDisableThreshold = DeploymentClusterPlanner.ClusterCPUCapacityDisableThreshold.valueIn(clusterId);
            final Float memoryDisableThreshold = DeploymentClusterPlanner.ClusterMemoryCapacityDisableThreshold.valueIn(clusterId);
            // Over commit ratios
            final Double cpuOvercommitRatio = findRatioValue(ApiDBUtils.findClusterDetails(clusterId, "cpuOvercommitRatio"));
            final Double memoryOvercommitRatio = findRatioValue(ApiDBUtils.findClusterDetails(clusterId, "memoryOvercommitRatio"));

            Long upInstances = 0L;
            Long totalInstances = 0L;
            for (final VMInstanceVO instance: vmInstanceDao.listByHostId(hostId)) {
                if (instance == null) {
                    continue;
                }
                if (instance.getType() == VirtualMachine.Type.User) {
                    totalInstances++;
                    if (instance.getState() == VirtualMachine.State.Running) {
                        upInstances++;
                    }
                }
            }
            metricsResponse.setPowerState(hostResponse.getOutOfBandManagementResponse().getPowerState());
            metricsResponse.setInstances(upInstances, totalInstances);
            metricsResponse.setCpuTotal(hostResponse.getCpuNumber(), hostResponse.getCpuSpeed(), cpuOvercommitRatio);
            metricsResponse.setCpuUsed(hostResponse.getCpuUsed(), hostResponse.getCpuNumber(), hostResponse.getCpuSpeed());
            metricsResponse.setCpuAllocated(hostResponse.getCpuAllocated(), hostResponse.getCpuNumber(), hostResponse.getCpuSpeed());
            metricsResponse.setLoadAverage(hostResponse.getAverageLoad());
            metricsResponse.setMemTotal(hostResponse.getMemoryTotal(), memoryOvercommitRatio);
            metricsResponse.setMemAllocated(hostResponse.getMemoryAllocated());
            metricsResponse.setMemUsed(hostResponse.getMemoryUsed());
            metricsResponse.setNetworkRead(hostResponse.getNetworkKbsRead());
            metricsResponse.setNetworkWrite(hostResponse.getNetworkKbsWrite());
            // CPU thresholds
            metricsResponse.setCpuUsageThreshold(hostResponse.getCpuUsed(), cpuThreshold);
            metricsResponse.setCpuUsageDisableThreshold(hostResponse.getCpuUsed(), cpuDisableThreshold);
            metricsResponse.setCpuAllocatedThreshold(hostResponse.getCpuAllocated(), cpuOvercommitRatio, cpuThreshold);
            metricsResponse.setCpuAllocatedDisableThreshold(hostResponse.getCpuAllocated(), cpuOvercommitRatio, cpuDisableThreshold);
            // Memory thresholds
            metricsResponse.setMemoryUsageThreshold(hostResponse.getMemoryUsed(), hostResponse.getMemoryTotal(), memoryThreshold);
            metricsResponse.setMemoryUsageDisableThreshold(hostResponse.getMemoryUsed(), hostResponse.getMemoryTotal(), memoryDisableThreshold);
            metricsResponse.setMemoryAllocatedThreshold(hostResponse.getMemoryAllocated(), hostResponse.getMemoryTotal(), memoryOvercommitRatio, memoryThreshold);
            metricsResponse.setMemoryAllocatedDisableThreshold(hostResponse.getMemoryAllocated(), hostResponse.getMemoryTotal(), memoryOvercommitRatio, memoryDisableThreshold);
            metricsResponses.add(metricsResponse);
        }
        return metricsResponses;
    }

    private CapacityDaoImpl.SummedCapacity getCapacity(final int capacityType, final Long zoneId, final Long clusterId) {
        final List<CapacityDaoImpl.SummedCapacity> capacities = capacityDao.findCapacityBy(capacityType, zoneId, null, clusterId);
        if (capacities == null || capacities.size() < 1) {
            return null;
        }
        return capacities.get(0);
    }

    @Override
    public List<ClusterMetricsResponse> listClusterMetrics(List<ClusterResponse> clusterResponses) {
        final List<ClusterMetricsResponse> metricsResponses = new ArrayList<>();
        for (final ClusterResponse clusterResponse: clusterResponses) {
            ClusterMetricsResponse metricsResponse = new ClusterMetricsResponse();

            try {
                BeanUtils.copyProperties(metricsResponse, clusterResponse);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to generate cluster metrics response");
            }

            final Cluster cluster = clusterDao.findByUuid(clusterResponse.getId());
            if (cluster == null) {
                continue;
            }
            final Long clusterId = cluster.getId();

            // Thresholds
            final Double cpuThreshold = AlertManager.CPUCapacityThreshold.valueIn(clusterId);
            final Double memoryThreshold = AlertManager.MemoryCapacityThreshold.valueIn(clusterId);
            final Float cpuDisableThreshold = DeploymentClusterPlanner.ClusterCPUCapacityDisableThreshold.valueIn(clusterId);
            final Float memoryDisableThreshold = DeploymentClusterPlanner.ClusterMemoryCapacityDisableThreshold.valueIn(clusterId);

            // CPU and memory capacities
            final CapacityDaoImpl.SummedCapacity cpuCapacity = getCapacity((int) Capacity.CAPACITY_TYPE_CPU, null, clusterId);
            final CapacityDaoImpl.SummedCapacity memoryCapacity = getCapacity((int) Capacity.CAPACITY_TYPE_MEMORY, null, clusterId);
            final Metrics metrics = new Metrics(cpuCapacity, memoryCapacity);

            for (final Host host: hostDao.findByClusterId(clusterId)) {
                if (host == null || host.getType() != Host.Type.Routing) {
                    continue;
                }
                if (host.getStatus() == Status.Up) {
                    metrics.incrUpResources();
                }
                metrics.incrTotalResources();
                updateHostMetrics(metrics, hostJoinDao.findById(host.getId()));
            }

            metricsResponse.setState(clusterResponse.getAllocationState(), clusterResponse.getManagedState());
            metricsResponse.setResources(metrics.getUpResources(), metrics.getTotalResources());
            // CPU
            metricsResponse.setCpuTotal(metrics.getTotalCpu());
            metricsResponse.setCpuAllocated(metrics.getCpuAllocated(), metrics.getTotalCpu());
            if (metrics.getCpuUsedPercentage() > 0L) {
                metricsResponse.setCpuUsed(metrics.getCpuUsedPercentage(), metrics.getTotalHosts());
                metricsResponse.setCpuMaxDeviation(metrics.getMaximumCpuUsage(), metrics.getCpuUsedPercentage(), metrics.getTotalHosts());
            }
            // Memory
            metricsResponse.setMemTotal(metrics.getTotalMemory());
            metricsResponse.setMemAllocated(metrics.getMemoryAllocated(), metrics.getTotalMemory());
            if (metrics.getMemoryUsed() > 0L) {
                metricsResponse.setMemUsed(metrics.getMemoryUsed(), metrics.getTotalMemory());
                metricsResponse.setMemMaxDeviation(metrics.getMaximumMemoryUsage(), metrics.getMemoryUsed(), metrics.getTotalHosts());
            }
            // CPU thresholds
            metricsResponse.setCpuUsageThreshold(metrics.getCpuUsedPercentage(), metrics.getTotalHosts(), cpuThreshold);
            metricsResponse.setCpuUsageDisableThreshold(metrics.getCpuUsedPercentage(), metrics.getTotalHosts(), cpuDisableThreshold);
            metricsResponse.setCpuAllocatedThreshold(metrics.getCpuAllocated(), metrics.getTotalCpu(), cpuThreshold);
            metricsResponse.setCpuAllocatedDisableThreshold(metrics.getCpuAllocated(), metrics.getTotalCpu(), cpuDisableThreshold);
            // Memory thresholds
            metricsResponse.setMemoryUsageThreshold(metrics.getMemoryUsed(), metrics.getTotalMemory(), memoryThreshold);
            metricsResponse.setMemoryUsageDisableThreshold(metrics.getMemoryUsed(), metrics.getTotalMemory(), memoryDisableThreshold);
            metricsResponse.setMemoryAllocatedThreshold(metrics.getMemoryAllocated(), metrics.getTotalMemory(), memoryThreshold);
            metricsResponse.setMemoryAllocatedDisableThreshold(metrics.getMemoryAllocated(), metrics.getTotalMemory(), memoryDisableThreshold);

            metricsResponses.add(metricsResponse);
        }
        return metricsResponses;
    }

    @Override
    public List<ZoneMetricsResponse> listZoneMetrics(List<ZoneResponse> zoneResponses) {
        final List<ZoneMetricsResponse> metricsResponses = new ArrayList<>();
        for (final ZoneResponse zoneResponse: zoneResponses) {
            ZoneMetricsResponse metricsResponse = new ZoneMetricsResponse();

            try {
                BeanUtils.copyProperties(metricsResponse, zoneResponse);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to generate zone metrics response");
            }

            final DataCenter zone = dataCenterDao.findByUuid(zoneResponse.getId());
            if (zone == null) {
                continue;
            }
            final Long zoneId = zone.getId();

            // Thresholds
            final Double cpuThreshold = AlertManager.CPUCapacityThreshold.value();
            final Double memoryThreshold = AlertManager.MemoryCapacityThreshold.value();
            final Float cpuDisableThreshold = DeploymentClusterPlanner.ClusterCPUCapacityDisableThreshold.value();
            final Float memoryDisableThreshold = DeploymentClusterPlanner.ClusterMemoryCapacityDisableThreshold.value();

            // CPU and memory capacities
            final CapacityDaoImpl.SummedCapacity cpuCapacity = getCapacity((int) Capacity.CAPACITY_TYPE_CPU, zoneId, null);
            final CapacityDaoImpl.SummedCapacity memoryCapacity = getCapacity((int) Capacity.CAPACITY_TYPE_MEMORY, zoneId, null);
            final Metrics metrics = new Metrics(cpuCapacity, memoryCapacity);

            for (final Cluster cluster : clusterDao.listClustersByDcId(zoneId)) {
                if (cluster == null) {
                    continue;
                }
                metrics.incrTotalResources();
                if (cluster.getAllocationState() == Grouping.AllocationState.Enabled
                        && cluster.getManagedState() == Managed.ManagedState.Managed) {
                    metrics.incrUpResources();
                }

                for (final Host host: hostDao.findByClusterId(cluster.getId())) {
                    if (host == null || host.getType() != Host.Type.Routing) {
                        continue;
                    }
                    updateHostMetrics(metrics, hostJoinDao.findById(host.getId()));
                }
            }

            metricsResponse.setState(zoneResponse.getAllocationState());
            metricsResponse.setResource(metrics.getUpResources(), metrics.getTotalResources());
            // CPU
            metricsResponse.setCpuTotal(metrics.getTotalCpu());
            metricsResponse.setCpuAllocated(metrics.getCpuAllocated(), metrics.getTotalCpu());
            if (metrics.getCpuUsedPercentage() > 0L) {
                metricsResponse.setCpuUsed(metrics.getCpuUsedPercentage(), metrics.getTotalHosts());
                metricsResponse.setCpuMaxDeviation(metrics.getMaximumCpuUsage(), metrics.getCpuUsedPercentage(), metrics.getTotalHosts());
            }
            // Memory
            metricsResponse.setMemTotal(metrics.getTotalMemory());
            metricsResponse.setMemAllocated(metrics.getMemoryAllocated(), metrics.getTotalMemory());
            if (metrics.getMemoryUsed() > 0L) {
                metricsResponse.setMemUsed(metrics.getMemoryUsed(), metrics.getTotalMemory());
                metricsResponse.setMemMaxDeviation(metrics.getMaximumMemoryUsage(), metrics.getMemoryUsed(), metrics.getTotalHosts());
            }
            // CPU thresholds
            metricsResponse.setCpuUsageThreshold(metrics.getCpuUsedPercentage(), metrics.getTotalHosts(), cpuThreshold);
            metricsResponse.setCpuUsageDisableThreshold(metrics.getCpuUsedPercentage(), metrics.getTotalHosts(), cpuDisableThreshold);
            metricsResponse.setCpuAllocatedThreshold(metrics.getCpuAllocated(), metrics.getTotalCpu(), cpuThreshold);
            metricsResponse.setCpuAllocatedDisableThreshold(metrics.getCpuAllocated(), metrics.getTotalCpu(), cpuDisableThreshold);
            // Memory thresholds
            metricsResponse.setMemoryUsageThreshold(metrics.getMemoryUsed(), metrics.getTotalMemory(), memoryThreshold);
            metricsResponse.setMemoryUsageDisableThreshold(metrics.getMemoryUsed(), metrics.getTotalMemory(), memoryDisableThreshold);
            metricsResponse.setMemoryAllocatedThreshold(metrics.getMemoryAllocated(), metrics.getTotalMemory(), memoryThreshold);
            metricsResponse.setMemoryAllocatedDisableThreshold(metrics.getMemoryAllocated(), metrics.getTotalMemory(), memoryDisableThreshold);

            metricsResponses.add(metricsResponse);
        }
        return metricsResponses;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(ListInfrastructureCmd.class);
        cmdList.add(ListVolumesMetricsCmd.class);
        cmdList.add(ListVMsMetricsCmd.class);
        cmdList.add(ListStoragePoolsMetricsCmd.class);
        cmdList.add(ListHostsMetricsCmd.class);
        cmdList.add(ListClustersMetricsCmd.class);
        cmdList.add(ListZonesMetricsCmd.class);
        return cmdList;
    }

    private class Metrics {
        // CPU metrics
        private Long totalCpu = 0L;
        private Long cpuAllocated = 0L;
        private Double cpuUsedPercentage = 0.0;
        private Double maximumCpuUsage = 0.0;
        // Memory metrics
        private Long totalMemory = 0L;
        private Long memoryUsed = 0L;
        private Long memoryAllocated = 0L;
        private Long maximumMemoryUsage = 0L;
        // Counters
        private Long totalHosts = 0L;
        private Long totalResources = 0L;
        private Long upResources = 0L;

        public Metrics(final CapacityDaoImpl.SummedCapacity totalCpu, final CapacityDaoImpl.SummedCapacity totalMemory) {
            if (totalCpu != null) {
                this.totalCpu = totalCpu.getTotalCapacity();
            }
            if (totalMemory != null) {
                this.totalMemory = totalMemory.getTotalCapacity();
            }
        }

        public void addCpuAllocated(Long cpuAllocated) {
            this.cpuAllocated += cpuAllocated;
        }

        public void addCpuUsedPercentage(Double cpuUsedPercentage) {
            this.cpuUsedPercentage += cpuUsedPercentage;
        }

        public void setMaximumCpuUsage(Double maximumCpuUsage) {
            if (this.maximumCpuUsage == null || (maximumCpuUsage != null && maximumCpuUsage > this.maximumCpuUsage)) {
                this.maximumCpuUsage = maximumCpuUsage;
            }
        }

        public void addMemoryUsed(Long memoryUsed) {
            this.memoryUsed += memoryUsed;
        }

        public void addMemoryAllocated(Long memoryAllocated) {
            this.memoryAllocated += memoryAllocated;
        }

        public void setMaximumMemoryUsage(Long maximumMemoryUsage) {
            if (this.maximumMemoryUsage == null || (maximumMemoryUsage != null && maximumMemoryUsage > this.maximumMemoryUsage)) {
                this.maximumMemoryUsage = maximumMemoryUsage;
            }
        }

        public void incrTotalHosts() {
            this.totalHosts++;
        }

        public void incrTotalResources() {
            this.totalResources++;
        }

        public void incrUpResources() {
            this.upResources++;
        }

        public Long getTotalCpu() {
            return totalCpu;
        }

        public Long getCpuAllocated() {
            return cpuAllocated;
        }

        public Double getCpuUsedPercentage() {
            return cpuUsedPercentage;
        }

        public Double getMaximumCpuUsage() {
            return maximumCpuUsage;
        }

        public Long getTotalMemory() {
            return totalMemory;
        }

        public Long getMemoryUsed() {
            return memoryUsed;
        }

        public Long getMemoryAllocated() {
            return memoryAllocated;
        }

        public Long getMaximumMemoryUsage() {
            return maximumMemoryUsage;
        }

        public Long getTotalHosts() {
            return totalHosts;
        }

        public Long getTotalResources() {
            return totalResources;
        }

        public Long getUpResources() {
            return upResources;
        }
    }

}
