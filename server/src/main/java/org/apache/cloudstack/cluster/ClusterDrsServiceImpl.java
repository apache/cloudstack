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
import com.cloud.api.query.vo.HostJoinVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.server.ManagementServer;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmService;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.cluster.GenerateClusterDrsPlanCmd;
import org.apache.cloudstack.cluster.dao.ClusterDrsEventsDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.managed.context.ManagedContextTimerTask;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import static com.cloud.org.Grouping.AllocationState.Disabled;
import static java.lang.Math.round;

public class ClusterDrsServiceImpl extends ManagerBase implements ClusterDrsService, PluggableService {

    private static final Logger logger = Logger.getLogger(ClusterDrsServiceImpl.class);

    @Inject
    ClusterDao clusterDao;

    @Inject
    CapacityDao capacityDao;

    @Inject
    HostDao hostDao;

    @Inject
    HostJoinDao hostJoinDao;

    @Inject
    VMInstanceDao vmInstanceDao;

    @Inject
    UserVmService userVmService;

    @Inject
    ClusterDrsEventsDao drsEventsDao;

    @Inject
    ServiceOfferingDao serviceOfferingDao;

    @Inject
    ManagementServer managementServer;

    List<ClusterDrsAlgorithm> drsAlgorithms = new ArrayList<>();

    Map<String, ClusterDrsAlgorithm> drsAlgorithmMap = new HashMap<>();

    public void setDrsAlgorithms(final List<ClusterDrsAlgorithm> drsAlgorithms) {
        this.drsAlgorithms = drsAlgorithms;
    }

    @Override
    public boolean start() {
        drsAlgorithmMap.clear();
        for (final ClusterDrsAlgorithm algorithm : drsAlgorithms) {
            drsAlgorithmMap.put(algorithm.getName(), algorithm);
        }

        final TimerTask schedulerPollTask = new ManagedContextTimerTask() {
            @Override
            protected void runInContext() {
                try {
                    poll(new Date());
                } catch (final Exception e) {
                    logger.error("Error while running DRS", e);
                }
            }
        };
        Timer vmSchedulerTimer = new Timer("VMSchedulerPollTask");
        vmSchedulerTimer.schedule(schedulerPollTask, 5000L, 60 * 1000L);
        return true;
    }

    @Override
    public void poll(Date timestamp) {
        Date currentTimestamp = DateUtils.round(timestamp, Calendar.MINUTE);
        String displayTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, currentTimestamp);
        logger.debug(String.format("VM scheduler.poll is being called at %s", displayTime));

        GlobalLock lock = GlobalLock.getInternLock("clusterDRS.poll");
        try {
            if (lock.lock(30)) {
                try {
                    executeDrsForAllClusters();
                } finally {
                    lock.unlock();
                }
            }
        } finally {
            lock.releaseRef();
        }
        GlobalLock cleanupLock = GlobalLock.getInternLock("clusterDRS.cleanup");
        try {
            if (cleanupLock.lock(30)) {
                try {
                    cleanUpOldDrsEvents();
                } finally {
                    cleanupLock.unlock();
                }
            }
        } finally {
            cleanupLock.releaseRef();
        }
    }

    private ClusterDrsAlgorithm getDrsAlgorithm(String algoName) {
        if (drsAlgorithmMap.containsKey(algoName)) {
            return drsAlgorithmMap.get(algoName);
        }
        throw new CloudRuntimeException("Invalid algorithm configured!");
    }

    @Override
    public String getConfigComponentName() {
        return ClusterDrsService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{ClusterDrsEventsExpireInterval, ClusterDrsEnabled, ClusterDrsInterval, ClusterDrsIterations, ClusterDrsAlgorithm, ClusterDrsThreshold, ClusterDrsMetric};
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(GenerateClusterDrsPlanCmd.class);
        return cmdList;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_CLUSTER_DRS, eventDescription = "Executing DRS", async = true)
    public List<Pair<Host, VirtualMachine>> generateDrsPlan(GenerateClusterDrsPlanCmd cmd) {
        Cluster cluster = clusterDao.findById(cmd.getId());
        if (cluster == null) {
            throw new InvalidParameterValueException("Unable to find the cluster by id=" + cmd.getId());
        }
        if (cluster.getAllocationState() == Disabled) {
            throw new InvalidParameterValueException(String.format("Unable to execute DRS on the cluster %s as it is disabled", cluster.getName()));
        }
        if (cluster.getClusterType() != Cluster.ClusterType.CloudManaged) {
            throw new InvalidParameterValueException(String.format("Unable to execute DRS on the cluster %s as it is not a cloud stack managed cluster", cluster.getName()));
        }
        if (cmd.getIterations() <= 0) {
            throw new InvalidParameterValueException(String.format("Unable to execute DRS on the cluster %s as the number of iterations [%s] is invalid", cluster.getName(), cmd.getIterations()));
        }

        try {
            return getDrsPlan(cluster, cmd.getIterations());
        } catch (ConfigurationException e) {
            throw new CloudRuntimeException("Unable to schedule DRS", e);
        }

    }

    int executeDrsPlan(List<Pair<Host, VirtualMachine>> plan) {
        // TODO: Create an async MigrateMultipleVM job instead
        int successCount = 0;
        for (Pair<Host, VirtualMachine> migration : plan) {
            if (migrateVM(migration.second(), migration.first())) {
                successCount++;
            }
        }
        return successCount;
    }

    /**
     * Executes DRS for the given cluster with the specified iteration percentage
     * and algorithm.
     *
     * @param cluster             The cluster to execute DRS on.
     * @param iterationPercentage The percentage of VMs to consider for migration
     *                            during each iteration.
     * @return The number of iterations executed.
     * @throws ConfigurationException If there is an error in the DRS configuration.
     */
    List<Pair<Host, VirtualMachine>> getDrsPlan(Cluster cluster, double iterationPercentage) throws ConfigurationException {
        // Take a lock on drs for a cluster to avoid automatic & ad hoc drs at the same
        // time on the same cluster
        GlobalLock lock = GlobalLock.getInternLock("cluster.drs." + cluster.getId());
        List<Pair<Host, VirtualMachine>> migrationPlan = new ArrayList<>();

        if (cluster.getAllocationState() == Disabled || cluster.getClusterType() != Cluster.ClusterType.CloudManaged || iterationPercentage <= 0) {
            return Collections.emptyList();
        }
        ClusterDrsAlgorithm algorithm = getDrsAlgorithm(ClusterDrsAlgorithm.valueIn(cluster.getId()));
        List<HostVO> hostList = hostDao.findByClusterId(cluster.getId());
        List<VirtualMachine> vmList = new ArrayList<>(vmInstanceDao.listByClusterId(cluster.getId()));

        int iteration = 0;
        long maxIterations = Math.max(round(iterationPercentage * vmList.size()), 1);

        Map<Long, List<VirtualMachine>> hostVmMap = getHostVmMap(hostList, vmList);
        Map<Long, List<Long>> originalHostIdVmIdMap = new HashMap<>();
        for (HostVO host : hostList) {
            originalHostIdVmIdMap.put(host.getId(), new ArrayList<>());
            for (VirtualMachine vm : hostVmMap.get(host.getId())) {
                originalHostIdVmIdMap.get(host.getId()).add(vm.getId());
            }
        }

        List<HostJoinVO> hostJoinList = hostJoinDao.searchByIds(hostList.stream().map(HostVO::getId).toArray(Long[]::new));

        Map<Long, Long> hostCpuCapacityMap = hostJoinList.stream().collect(Collectors.toMap(HostJoinVO::getId, HostJoinVO::getCpuUsedCapacity));
        Map<Long, Long> hostMemoryCapacityMap = hostJoinList.stream().collect(Collectors.toMap(HostJoinVO::getId, HostJoinVO::getMemUsedCapacity));

        while (iteration < maxIterations && algorithm.needsDrs(cluster.getId(), new ArrayList<>(hostCpuCapacityMap.values()), new ArrayList<>(hostMemoryCapacityMap.values()))) {
            Pair<Host, VirtualMachine> bestMigration = getBestMigration(cluster, algorithm, vmList, hostVmMap, hostCpuCapacityMap, hostMemoryCapacityMap);
            Host destHost = bestMigration.first();
            VirtualMachine vm = bestMigration.second();
            if (destHost == null || vm == null || originalHostIdVmIdMap.get(destHost.getId()).contains(vm.getId())) {
                logger.debug("VM migrating to it's original host or no host found for migration");
                break;
            }

            ServiceOffering serviceOffering = serviceOfferingDao.findByIdIncludingRemoved(vm.getId(), vm.getServiceOfferingId());

            hostVmMap = getHostVmMapAfterMigration(hostVmMap, vm, vm.getHostId(), destHost.getId());
            hostVmMap.get(vm.getHostId()).remove(vm);
            hostVmMap.get(destHost.getId()).add(vm);

            hostCpuCapacityMap.put(vm.getHostId(), hostCpuCapacityMap.get(vm.getHostId()) - serviceOffering.getCpu());
            hostCpuCapacityMap.put(destHost.getId(), hostCpuCapacityMap.get(destHost.getId()) + serviceOffering.getCpu());
            hostMemoryCapacityMap.put(vm.getHostId(), hostMemoryCapacityMap.get(vm.getHostId()) - serviceOffering.getRamSize());
            hostMemoryCapacityMap.put(destHost.getId(), hostMemoryCapacityMap.get(destHost.getId()) + serviceOffering.getRamSize());
            vm.setHostId(destHost.getId());
            migrationPlan.add(bestMigration);
            iteration++;
        }
        return migrationPlan;
    }

    // TODO: Create an async job and track the job status
    private boolean migrateVM(VirtualMachine vm, Host destHost) {
        try {
            CallContext.current().setEventResourceId(vm.getId());
            CallContext.current().setEventResourceType(ApiCommandResourceType.VirtualMachine);

            VirtualMachine newVm = userVmService.migrateVirtualMachine(vm.getId(), destHost);
            if (newVm.getHostId() != destHost.getId()) {
                return false;
            }

            logger.debug("Migrated VM " + vm.getInstanceName() + " from host " + vm.getHostId() + " to host " + destHost.getId());
            return true;
        } catch (ResourceUnavailableException e) {
            logger.warn("Exception: ", e);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, e.getMessage());
        } catch (VirtualMachineMigrationException | ConcurrentOperationException | ManagementServerException e) {
            logger.warn("Exception: ", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    Map<Long, List<VirtualMachine>> getHostVmMapAfterMigration(Map<Long, List<VirtualMachine>> hostVmMap, VirtualMachine vm, long srcHostId, long destHostId) {
        hostVmMap.get(srcHostId).remove(vm);
        hostVmMap.get(destHostId).add(vm);
        return hostVmMap;
    }

    Map<Long, List<VirtualMachine>> getHostVmMap(List<HostVO> hostList, List<VirtualMachine> vmList) {
        Map<Long, List<VirtualMachine>> hostVmMap = new HashMap<>();
        for (HostVO host : hostList) {
            hostVmMap.put(host.getId(), new ArrayList<>());
        }
        for (VirtualMachine vm : vmList) {
            hostVmMap.get(vm.getHostId()).add(vm);
        }
        return hostVmMap;
    }

    /**
     * Returns the best migration for the given cluster, algorithm, VM list and host
     * VM map.
     *
     * @param cluster   The cluster to execute DRS on.
     * @param algorithm The DRS algorithm to use.
     * @param vmList    The list of VMs to consider for migration.
     * @param hostVmMap The map of hosts to VMs.
     * @return the best migration for the given cluster, algorithm, VM list and host
     */
    Pair<Host, VirtualMachine> getBestMigration(Cluster cluster, ClusterDrsAlgorithm algorithm, List<VirtualMachine> vmList, Map<Long, List<VirtualMachine>> hostVmMap, Map<Long, Long> hostCpuCapacityMap, Map<Long, Long> hostMemoryCapacityMap) {
        double maxImprovement = 0;
        Pair<Host, VirtualMachine> bestMigration = new Pair<>(null, null);

        for (VirtualMachine vm : vmList) {
            Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>> hostsForMigrationOfVM = managementServer.listHostsForMigrationOfVM(vm, 0L, (long) hostVmMap.size(), null, vmList);
            List<? extends Host> compatibleDestinationHosts = hostsForMigrationOfVM.second();
            Map<Host, Boolean> requiresStorageMotion = hostsForMigrationOfVM.third();

            for (Host destHost : compatibleDestinationHosts) {
                Ternary<Double, Double, Double> metrics = algorithm.getMetrics(cluster.getId(), vm, destHost, hostCpuCapacityMap, hostMemoryCapacityMap, requiresStorageMotion.get(destHost));

                Double improvement = metrics.first();
                Double cost = metrics.second();
                Double benefit = metrics.third();
                if (benefit > cost && (improvement > maxImprovement)) {
                    bestMigration = new Pair<>(destHost, vm);
                    maxImprovement = improvement;
                }
            }
        }
        return bestMigration;
    }

    /**
     * Removes old DRS events that have expired based on the configured interval.
     */
    private void cleanUpOldDrsEvents() {
        int rowsRemoved = drsEventsDao.removeDrsEventsBeforeInterval(ClusterDrsEventsExpireInterval.value());
        logger.debug("Removed " + rowsRemoved + " old DRS events");
    }

    /**
     * Executes DRS for all clusters that meet the criteria for automated DRS.
     */
    private void executeDrsForAllClusters() {
        List<ClusterVO> clusterList = clusterDao.listAll();

        for (ClusterVO cluster : clusterList) {
            if (cluster.getAllocationState() == Disabled || cluster.getClusterType() != Cluster.ClusterType.CloudManaged || ClusterDrsEnabled.valueIn(cluster.getId()).equals(Boolean.FALSE) || drsEventsDao.lastAutomatedDrsEventInInterval(cluster.getId(), ClusterDrsInterval.valueIn(cluster.getId())) != null) {
                continue;
            }

            long eventId = ActionEventUtils.onStartedActionEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_CLUSTER_DRS, String.format("Executing automated DRS for cluster %s", cluster.getUuid()), cluster.getId(), ApiCommandResourceType.Cluster.toString(), true, 0);
            CallContext.current().setStartEventId(eventId);
            try {
                List<Pair<Host, VirtualMachine>> plan = getDrsPlan(cluster, ClusterDrsIterations.valueIn(cluster.getId()));
                int iterations = executeDrsPlan(plan);
                logger.debug(String.format("Executed %d iterations of DRS for cluster %s [id=%s]", iterations, cluster.getName(), cluster.getUuid()));
                drsEventsDao.persist(new ClusterDrsEventsVO(cluster.getId(), eventId, new Date(), iterations, ClusterDrsEvents.Type.AUTOMATED, ClusterDrsEvents.Result.SUCCESS));

                ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventVO.LEVEL_INFO, EventTypes.EVENT_CLUSTER_DRS, true, String.format("Executed %s iterations as part of automatic DRS for cluster %s", iterations, cluster.getName()), cluster.getId(), ApiCommandResourceType.Cluster.toString(), eventId);
            } catch (ConfigurationException e) {
                drsEventsDao.persist(new ClusterDrsEventsVO(cluster.getId(), eventId, new Date(), null, ClusterDrsEvents.Type.AUTOMATED, ClusterDrsEvents.Result.FAILURE));
                logger.error(String.format("Unable to execute DRS on cluster %s [id=%s]", cluster.getName(), cluster.getUuid()), e);
            }
        }
    }
}
