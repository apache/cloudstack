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
package com.cloud.hypervisor.vmware.drs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.api.command.admin.host.ListHostsCmd;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.query.QueryService;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.configuration.Config;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.vmware.drs.resource.HostResources;
import com.cloud.hypervisor.vmware.drs.resource.VmResources;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.vm.UserVmService;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;

@Component
public class VmwareDrsInternal extends ManagerBase implements Configurable {

    @Inject
    private QueryService queryService;
    @Inject
    private HostDao hostDao;
    @Inject
    private VMInstanceDao vmInstanceDao;
    @Inject
    private ServiceOfferingDao serviceOfferingDao;
    @Inject
    private ConfigurationDao configurationDao;
    @Inject
    private UserVmService userVmService;
    @Inject
    private ClusterDao clusterDao;
    @Inject
    private DataCenterDao dataCenterDao;
    @Inject
    private ClusterDetailsDao clusterDetailsDao;

    public static final Logger s_logger = Logger.getLogger(VmwareDrsInternal.class.getName());

    /**
     * Setting 'vmware.drs.internal.enabled' defined at global and cluster level:
     * <ul>
     * <li>If global setting = false -> DRS tasks shutdown</li>
     * <li>If global setting = true -> One DRS task per cluster is active every {@link Config#VmwareDRSInterval} seconds</li>
     * <ul>
     * <li>If cluster setting = true -> DRS task processes cluster to balance load within its hosts if necessary</li>
     * <li>If cluster setting = false -> No DRS process on cluster</li>
     * </ul>
     * </ul>
     */
    private final ConfigKey<Boolean> VmwareDrsInternalEnabled = new ConfigKey<Boolean>(Boolean.class, "vmware.drs.internal.enabled", "Advanced", "false",
            "Specify whether or not to enable DRS internal task on cluster.", true, ConfigKey.Scope.Cluster, null);

    /**
     * DRS scheduler, creates thread pool for DRS tasks for processing each active Vmware cluster. It works along with {@link #VmwareDrsInternalEnabled} setting:
     * <ul>
     * <li>If global setting = false -> no thread pool created</li>
     * <li>If global setting = true -> thread pool created, size = number of active Vmware clusters</li>
     * </ul>
     */
    private ScheduledExecutorService _drsScheduler = null;

    private static final int STARTUP_DELAY = 140;
    private static final int DEFAULT_DRS_INTERVAL_SECONDS = 60;
    private static final double DEFAULT_DRS_THRESHOLD = 0.20;

    /**
     * DRS Migration Threshold defined on global setting {@link Config#VmwareDRSThreshold}
     */
    protected static double drsMigrationThreshold = 0d;

    private StandardDeviation std = new StandardDeviation(false);

    private enum HostResourceOperation {ADD, SUBSTRACT};

    /**
     * Retrieve cluster host resources
     * @param clusterId cluster id
     * @return list of host resources
     */
    private List<HostResources> getClusterHostsResources(long clusterId) {
        List<HostResponse> hostsMetrics = getClusterHostsMetrics(clusterId);
        List<HostResources> hostResources = getHostResourcesFromMetrics(hostsMetrics);
        return hostResources;
    }

    /**
     * Retrieve hosts CPU usage metrics
     * @return hosts metrics
     */
    protected List<HostResponse> getClusterHostsMetrics(long clusterId) {
        ListHostsCmd cmd = new ListHostsCmd();
        cmd.setType(Host.Type.Routing.toString());
        cmd.setState("Up");
        cmd.setClusterId(clusterId);
        ListResponse<HostResponse> responses = queryService.searchForServers(cmd);
        return responses.getResponses();
    }

    /**
     * Get host resources from host metrics
     * @param hostsMetrics metrics
     * @return list of host resources
     */
    protected List<HostResources> getHostResourcesFromMetrics(List<HostResponse> hostsMetrics) {
        List<HostResources> hostResources = new ArrayList<HostResources>();
        for (HostResponse hostMetrics : hostsMetrics) {
            HostResources h = createHostResourceFromMetrics(hostMetrics);
            hostResources.add(h);
        }
        return hostResources;
    }

    /**
     * Retrieve host resource from host metric
     * @param hostMetrics host metric
     * @return host resource
     */
    private HostResources createHostResourceFromMetrics(HostResponse hostMetrics) {
        String cpuUsedPercentage = hostMetrics.getCpuUsed().substring(0, hostMetrics.getCpuUsed().length() - 1);
        double cpuUsage = Double.valueOf(cpuUsedPercentage) / 100;
        HostVO hostVO = hostDao.findByUuid(hostMetrics.getId());
        long totalCpuCapacityMhz = hostVO.getCpus() * hostVO.getSpeed();
        double cpuUsedMhz = totalCpuCapacityMhz * cpuUsage;

        List<VMInstanceVO> hostVms = vmInstanceDao.listByHostId(hostVO.getId());
        List<VmResources> hostVmResources = new ArrayList<VmResources>();
        for (VMInstanceVO vmInstanceVO : hostVms) {
            VmResources vmResource = createVmResource(vmInstanceVO);
            hostVmResources.add(vmResource);
        }
        return new HostResources(hostVO.getId(), cpuUsedMhz, hostVO.getCpus(), hostVO.getSpeed(), cpuUsage,
                hostMetrics.getMemoryUsed(), hostVO.getTotalMemory(), hostVmResources);
    }

    /**
     * Create {@link VmResources} from vm instance vo
     * @param vmInstanceVO vm instance vo
     * @return vm resource
     */
    private VmResources createVmResource(VMInstanceVO vmInstanceVO) {
        ServiceOfferingVO serviceOfferingVO = serviceOfferingDao.findById(vmInstanceVO.getServiceOfferingId());
        Integer cpuSpeed = serviceOfferingVO.getSpeed();
        Integer cpu = serviceOfferingVO.getCpu();
        Integer memory = serviceOfferingVO.getRamSize();
        return new VmResources(vmInstanceVO.getId(), cpu, cpuSpeed, memory);
    }

    /**
     * Get CPU usage array from host resources, preserving indexes between hostResources and returned array
     * @param hostResources host resources
     * @return CPU hosts usage array - usage values between 0 and 1
     */
    private double[] getHostsCPUUsage(List<HostResources> hostResources) {
        double[] hostsUsage = new double[hostResources.size()];
        for (int i = 0; i < hostResources.size(); i++) {
            HostResources hostResource = hostResources.get(i);
            hostsUsage[i] = hostResource.getNormalizedCpuUsage();
        }
        return hostsUsage;
    }

    /**
     * Check if cluster with id {@code clusterId} is meant to be managed by DRS task.<br/>
     * Cluster setting {@link #VmwareDrsInternalEnabled} should be true for cluster to be managed
     * @return true if cluster is managed, false if not
     */
    private boolean isClusterManaged(long clusterId) {
        Map<String, String> details = clusterDetailsDao.findDetails(clusterId);
        if (details.containsKey(VmwareDrsInternalEnabled.key())) {
            String localValue = details.get(VmwareDrsInternalEnabled.key());
            return Boolean.valueOf(localValue);
        }
        return true;
    }

    /**
     * Copy host resources list
     * @param hostResources host resources list
     * @return copied host resources list
     */
    private List<HostResources> copyList(List<HostResources> hostResources) {
        return new ArrayList<HostResources>(hostResources);
    }

    /**
     * Internal DRS task running for each enabled Vmware cluster
     *
     */
    class VmwareDrsTask extends ManagedContextRunnable {

        private long clusterId;

        public VmwareDrsTask(long clusterId) {
            this.clusterId = clusterId;
        }

        @Override
        protected void runInContext() {
            if (isClusterManaged(clusterId)) {
                try {
                    List<HostResources> hostsResources = getClusterHostsResources(clusterId);
                    if (! hostsResources.isEmpty() && hostsResources.size() > 1) {
                        processCluster(hostsResources);
                    }
                }
                catch (Exception e) {
                    s_logger.error("Failure while processing cluster " + clusterId + ": " + e.getMessage());
                }
            }
        }

        /**
         * Process cluster to balance the load if necessary, according to the following algorithm:
         * <ol>
         * <li>Calculate cluster imbalance (given by the standard deviation of hosts CPU usage - CPU usage between 0 and 1)</li>
         * <li>While cluster imbalance > {@linkplain VmwareDrsInternal#drsMigrationThreshold}:</li>
         * <ul>
         * <li>Get best vm move between hosts given by {@link #getBestMove(List, double[], Map)}</li>
         * <ul>
         * <li>If no good migration found -> stop processing cluster</li>
         * <li>If migration found -> add recommendation to recommendations list</li>
         * </ul>
         * </ul>
         * <li>Apply recommendations {@link #applyRecommendations(Map, double)}</li>
         * </ol>
         * @param hostsResources host resources
         */
        private void processCluster(List<HostResources> hostsResources) {
            double[] hostsCpuUsage = getHostsCPUUsage(hostsResources);
            double clusterImbalance = std.evaluate(hostsCpuUsage);
            Map<Long, Long> recommendations = new HashMap<Long, Long>();
            while (clusterImbalance > drsMigrationThreshold) {
                VmwareDrsInternalBestMove bestMove = getBestMove(hostsResources, hostsCpuUsage, recommendations);
                clusterImbalance = std.evaluate(hostsCpuUsage);
                if (bestMove == null) {
                    break;
                }
                else {
                    addRecommendation(bestMove, recommendations);
                }
            }
            applyRecommendations(recommendations, clusterImbalance);
        }

        /**
         * Add recommendation into {@code recommendations} list
         * @param bestMove best move recommendation
         * @param recommendations recommendations list
         */
        private void addRecommendation(VmwareDrsInternalBestMove bestMove, Map<Long, Long> recommendations) {
            long vmId = bestMove.getVmId();
            long hostId = bestMove.getHostId();
            recommendations.put(vmId, hostId);
        }

        /**
         * Apply {@code recommendations} on the system
         * @param recommendations recommendations list
         * @param cpuStdDev calculated CPU standard deviation
         */
        private void applyRecommendations(Map<Long, Long> recommendations, double cpuStdDev) {
            if (! recommendations.isEmpty()) {
                for (Long vmId : recommendations.keySet()) {
                    Long hostId = recommendations.get(vmId);
                    HostVO hostVO = hostDao.findById(hostId);
                    try {
                        userVmService.migrateVirtualMachine(vmId, hostVO);
                    } catch (Exception e) {
                        s_logger.error("Couldn't migrate vm " + vmId + " to host " + hostVO.getName() + "(" +
                                hostVO.getId() + ") on cluster: " + clusterId + " due to:" + e.getMessage());
                    }
                }
                s_logger.debug("Done applying " + recommendations.size() + " recommendations on cluster " + clusterId);
            }
        }

        /**
         * Return {@link VmwareDrsInternalBestMove} vm move that gives least cluster-wide imbalance
         * @param hostResources host resources
         * @param cpuLoad hosts CPU usage array
         * @param recommendations recommendations map
         * @return vm move.
         */
        private VmwareDrsInternalBestMove getBestMove(List<HostResources> hostResources, double[] cpuLoad, Map<Long, Long> recommendations) {
            List<HostResources> candidateHosts = copyList(hostResources);
            VmwareDrsInternalBestMove bestVmMove = new VmwareDrsInternalBestMove(std.evaluate(cpuLoad));
            for (int hostFromIndex = 0; hostFromIndex < hostResources.size(); hostFromIndex++) {
                HostResources hostFrom = hostResources.get(hostFromIndex);
                List<VmResources> hostFromVms = hostFrom.getVms();
                for (int vmIndex = 0; vmIndex < hostFromVms.size(); vmIndex++) {
                    for (int candidateHostIndex = 0; candidateHostIndex < candidateHosts.size(); candidateHostIndex++) {
                        VmResources vmResource = hostFromVms.get(vmIndex);
                        HostResources candidateHost = candidateHosts.get(candidateHostIndex);
                        if (candidateHost.getId() == hostFrom.getId()) {
                            continue;
                        }
                        if (canMigrateVmToHost(vmResource, candidateHost) && ! recommendations.containsKey(vmResource.getId())) {
                            double newStdDev = simulateVmMigrationToHost(vmResource, hostFrom, candidateHost, hostFromIndex, candidateHostIndex, cpuLoad);
                            if (newStdDev < bestVmMove.getStdDev()) {
                                updateBestMove(bestVmMove, hostFromIndex, candidateHostIndex, vmIndex, vmResource, candidateHost, newStdDev);
                            }
                        }
                    }
                }
            }
            if (bestVmMove.isUpdated()) {
                updateHostResourcesForBestMove(hostResources, cpuLoad, bestVmMove);
            }
            else {
                return null;
            }
            return bestVmMove;
        }

        /**
         * Update host resources usage given the best vm migration
         * @param hostResources host resources
         * @param cpuLoad cpu usage array
         * @param bestVmMove best vm migration
         */
        private void updateHostResourcesForBestMove(List<HostResources> hostResources, double[] cpuLoad, VmwareDrsInternalBestMove bestVmMove) {
            HostResources hostFrom = hostResources.get(bestVmMove.getHostFromIndex());
            HostResources hostTo = hostResources.get(bestVmMove.getHostToIndex());
            updateHostResources(hostFrom, HostResourceOperation.SUBSTRACT, bestVmMove.getVm(), bestVmMove.getVmIndex());
            updateHostResources(hostTo, HostResourceOperation.ADD, bestVmMove.getVm(), bestVmMove.getVmIndex());
            cpuLoad[bestVmMove.getHostFromIndex()] = hostFrom.getCpuUsedMhz() / hostFrom.getTotalCpuSpeedMhz();
            cpuLoad[bestVmMove.getHostToIndex()] = hostTo.getCpuUsedMhz() / hostTo.getTotalCpuSpeedMhz();
        }

        /**
         * Update host resources by adding/substracting vm resources
         * @param host host resources to update
         * @param operation add or substract enum
         * @param vm vm resources
         */
        private void updateHostResources(HostResources host, HostResourceOperation operation, VmResources vm, int vmIndex) {
            int vmCpuUsageMhz = vm.getCpu() * vm.getCpuSpeed();
            int vmMemoryUsageMb = vm.getMemory();
            if (operation.equals(HostResourceOperation.ADD)) {
                host.setCpuUsedMhz(host.getCpuUsedMhz() + vmCpuUsageMhz);
                host.setMemoryUsedMb(host.getMemoryUsedMb() + vmMemoryUsageMb);
                host.getVms().add(vm);
            }
            else if (operation.equals(HostResourceOperation.SUBSTRACT)) {
                host.setCpuUsedMhz(host.getCpuUsedMhz() - vmCpuUsageMhz);
                host.setMemoryUsedMb(host.getMemoryUsedMb() - vmMemoryUsageMb);
                host.getVms().remove(vmIndex);
            }
            host.setNormalizedCpuUsage(host.getCpuUsedMhz() / host.getTotalCpuSpeedMhz());
        }

        /**
         * Update best move migration
         * @param bestVmMove best move to update
         * @param hostFromInd host index
         * @param candidateHostIndex host index
         * @param vmIndex vm index
         * @param vmResource vm resource
         * @param candidateHost candidate host
         * @param newStdDev new std deviation
         */
        private void updateBestMove(VmwareDrsInternalBestMove bestVmMove, int hostFromInd, int candidateHostIndex, int vmIndex, VmResources vmResource, HostResources candidateHost, double newStdDev) {
            bestVmMove.setHostFromIndex(hostFromInd);
            bestVmMove.setHostToIndex(candidateHostIndex);
            bestVmMove.setVmIndex(vmIndex);
            bestVmMove.setVm(copyVmResource(vmResource));
            bestVmMove.setVmId(vmResource.getId());
            bestVmMove.setHostId(candidateHost.getId());
            bestVmMove.setUpdated(true);
            bestVmMove.setStdDev(newStdDev);
        }

        /**
         * Copy vm resource
         * @param vmResource vm resource
         * @return vm resource copy
         */
        private VmResources copyVmResource(VmResources vmResource) {
            return new VmResources(vmResource.getId(), vmResource.getCpu(), vmResource.getCpuSpeed(), vmResource.getMemory());
        }

        /**
         * Simulate vm migration
         * @param vmResource vm resource
         * @param hostFrom host form
         * @param candidateHost candidate host
         * @param hostFromIndex host from index
         * @param candidateHostIndex candidate host index
         * @param cpuLoad cpu usage array
         * @return std deviation after simulation
         */
        private double simulateVmMigrationToHost(VmResources vmResource, HostResources hostFrom, HostResources candidateHost, Integer hostFromIndex, int candidateHostIndex, double[] cpuLoad) {
            double candidateHostCpuLoad = cpuLoad[candidateHostIndex];
            double hostFromCpuLoad = cpuLoad[hostFromIndex];
            double vmCpuNeeds = vmResource.getCpu() * vmResource.getCpuSpeed();
            cpuLoad[candidateHostIndex] = (candidateHost.getCpuUsedMhz() + vmCpuNeeds) / candidateHost.getTotalCpuSpeedMhz();
            cpuLoad[hostFromIndex] = (hostFrom.getCpuUsedMhz() - vmCpuNeeds) / hostFrom.getTotalCpuSpeedMhz();
            double newStdDev = std.evaluate(cpuLoad);
            cpuLoad[hostFromIndex] = hostFromCpuLoad;
            cpuLoad[candidateHostIndex] = candidateHostCpuLoad;
            return newStdDev;
        }

        /**
         * Check if it is possible to migrate vmResource to candidateHost
         * @param vmResource vm to migrate
         * @param candidateHost host
         * @return true if vm can be migrated to host, false if not
         */
        private boolean canMigrateVmToHost(VmResources vmResource, HostResources candidateHost) {
            if (candidateHost.getCpus() < vmResource.getCpu()) {
                return false;
            }
            long vmCpuNeedsMhz = vmResource.getCpu() * vmResource.getCpuSpeed();
            if (candidateHost.getTotalCpuSpeedMhz() - candidateHost.getCpuUsedMhz() < vmCpuNeedsMhz) {
                return false;
            }
            if (candidateHost.getMemoryTotalMb() - candidateHost.getMemoryUsedMb() < vmResource.getMemory()) {
                return false;
            }
            return true;
        }

    }

    @Override
    public String getConfigComponentName() {
        return VmwareDrsInternal.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {VmwareDrsInternalEnabled};
    }

    @Override
    public boolean start() {
        Map<String, String> configurations = configurationDao.getConfiguration();
        String drsInterval = configurations.get(Config.VmwareDRSInterval.key());
        int drsIntervalSeconds = NumbersUtil.parseInt(drsInterval, DEFAULT_DRS_INTERVAL_SECONDS);
        String drsThresholdString = configurations.get(Config.VmwareDRSThreshold.key());
        drsMigrationThreshold = (drsThresholdString != null ? Double.parseDouble(drsThresholdString) : DEFAULT_DRS_THRESHOLD);

        String globalDrsConfigValue = configurations.get(VmwareDrsInternalEnabled.key());
        boolean globalDrsValue = Boolean.valueOf(globalDrsConfigValue);
        if (globalDrsValue) {
            List<DataCenterVO> enabledZones = dataCenterDao.listEnabledZones();
            List<ClusterVO> clustersToManage = new ArrayList<ClusterVO>();
            for (DataCenterVO dc : enabledZones) {
                List<ClusterVO> zoneClusters = clusterDao.listClustersByDcId(dc.getId());
                List<ClusterVO> enabledVmwareClusters = zoneClusters.stream().
                        filter(c -> c.getRemoved() == null && c.getHypervisorType().equals(HypervisorType.VMware)).collect(Collectors.toList());
                clustersToManage.addAll(enabledVmwareClusters);
            }

            _drsScheduler = Executors.newScheduledThreadPool(clustersToManage.size(), new NamedThreadFactory("Vmware-DRS-Internal"));


            for (ClusterVO clusterVO : clustersToManage) {
                _drsScheduler.scheduleWithFixedDelay(new VmwareDrsTask(clusterVO.getId()), STARTUP_DELAY, drsIntervalSeconds, TimeUnit.SECONDS);
            }
        }

        return true;
    }

    @Override
    public boolean stop() {
        _drsScheduler.shutdown();
        return true;
    }
}
