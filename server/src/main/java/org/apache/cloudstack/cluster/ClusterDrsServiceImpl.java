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

import com.cloud.api.ApiGsonHelper;
import com.cloud.api.query.dao.HostJoinDao;
import com.cloud.api.query.vo.HostJoinVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.exception.InvalidParameterValueException;
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
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmService;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.cluster.GenerateClusterDrsPlanCmd;
import org.apache.cloudstack.api.command.admin.vm.MigrateMultipleVMsCmd;
import org.apache.cloudstack.cluster.dao.ClusterDrsEventsDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.managed.context.ManagedContextTimerTask;
import org.apache.commons.collections.MapUtils;
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

    AsyncJobDispatcher asyncJobDispatcher;

    @Inject
    AsyncJobManager asyncJobManager;

    @Inject
    ClusterDao clusterDao;

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

    public AsyncJobDispatcher getAsyncJobDispatcher() {
        return asyncJobDispatcher;
    }

    public void setAsyncJobDispatcher(final AsyncJobDispatcher dispatcher) {
        asyncJobDispatcher = dispatcher;
    }

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
        logger.debug(String.format("ClusterDRS.poll is being called at %s", displayTime));

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
        return new ConfigKey<?>[]{ClusterDrsEventsExpireInterval, ClusterDrsEnabled, ClusterDrsInterval, ClusterDrsIterations, ClusterDrsAlgorithm, ClusterDrsLevel, ClusterDrsMetric};
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(GenerateClusterDrsPlanCmd.class);
        return cmdList;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_CLUSTER_DRS, eventDescription = "Generating DRS plan", async = true)
    public List<Ternary<VirtualMachine, Host, Host>> generateDrsPlan(GenerateClusterDrsPlanCmd cmd) {
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

    long executeDrsPlan(Long clusterId, List<Ternary<VirtualMachine, Host, Host>> plan) {
        List<Long> destHostIdList = new ArrayList<>();
        List<Long> vmIdList = new ArrayList<>();
        for (Ternary<VirtualMachine, Host, Host> migration : plan) {
            destHostIdList.add(migration.third().getId());
            vmIdList.add(migration.first().getId());
        }
        final Map<String, String> params = new HashMap<>();
        params.put("ctxUserId", String.valueOf(User.UID_SYSTEM));
        params.put("ctxAccountId", String.valueOf(Account.ACCOUNT_ID_SYSTEM));
        params.put(ApiConstants.CTX_START_EVENT_ID, String.valueOf(CallContext.current().getStartEventId()));
        params.put(ApiConstants.HOST_IDS, destHostIdList.toString());
        params.put(ApiConstants.VIRTUAL_MACHINE_IDS, vmIdList.toString());

        final MigrateMultipleVMsCmd cmd = new MigrateMultipleVMsCmd();
        ComponentContext.inject(cmd);

        AsyncJobVO job = new AsyncJobVO("", User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, MigrateMultipleVMsCmd.class.getName(), ApiGsonHelper.getBuilder().create().toJson(params), clusterId, ApiCommandResourceType.Cluster.toString(), null);
        job.setDispatcher(asyncJobDispatcher.getName());

        return asyncJobManager.submitAsyncJob(job);
    }

    /**
     * Generate DRS plan for the given cluster with the specified iteration percentage.
     *
     * @param cluster             The cluster to execute DRS on.
     * @param iterationPercentage The percentage of VMs to consider for migration
     *                            during each iteration.
     * @return List of Ternary object containing VM to be migrated, source host and destination host.
     * @throws ConfigurationException If there is an error in the DRS configuration.
     */
    List<Ternary<VirtualMachine, Host, Host>> getDrsPlan(Cluster cluster, double iterationPercentage) throws ConfigurationException {
        List<Ternary<VirtualMachine, Host, Host>> migrationPlan = new ArrayList<>();

        if (cluster.getAllocationState() == Disabled || cluster.getClusterType() != Cluster.ClusterType.CloudManaged || iterationPercentage <= 0) {
            return Collections.emptyList();
        }
        ClusterDrsAlgorithm algorithm = getDrsAlgorithm(ClusterDrsAlgorithm.valueIn(cluster.getId()));
        List<HostVO> hostList = hostDao.findByClusterId(cluster.getId());
        List<VirtualMachine> vmList = new ArrayList<>(vmInstanceDao.listByClusterId(cluster.getId()));

        int iteration = 0;
        long maxIterations = Math.max(round(iterationPercentage * vmList.size()), 1);

        Map<Long, Host> hostMap = hostList.stream().collect(Collectors.toMap(HostVO::getId, host -> host));

        Map<Long, List<VirtualMachine>> hostVmMap = getHostVmMap(hostList, vmList);
        Map<Long, List<Long>> originalHostIdVmIdMap = new HashMap<>();
        for (HostVO host : hostList) {
            originalHostIdVmIdMap.put(host.getId(), new ArrayList<>());
            for (VirtualMachine vm : hostVmMap.get(host.getId())) {
                originalHostIdVmIdMap.get(host.getId()).add(vm.getId());
            }
        }

        List<HostJoinVO> hostJoinList = hostJoinDao.searchByIds(hostList.stream().map(HostVO::getId).toArray(Long[]::new));

        Map<Long, Long> hostCpuMap = hostJoinList.stream().collect(Collectors.toMap(HostJoinVO::getId, hostJoin -> hostJoin.getCpuUsedCapacity() + hostJoin.getCpuReservedCapacity()));
        Map<Long, Long> hostMemoryMap = hostJoinList.stream().collect(Collectors.toMap(HostJoinVO::getId, hostJoin -> hostJoin.getMemUsedCapacity() + hostJoin.getMemReservedCapacity()));

        Map<Long, ServiceOffering> vmIdServiceOfferingMap = new HashMap<>();

        for (VirtualMachine vm : vmList) {
            vmIdServiceOfferingMap.put(vm.getId(), serviceOfferingDao.findByIdIncludingRemoved(vm.getId(), vm.getServiceOfferingId()));
        }

        while (iteration < maxIterations && algorithm.needsDrs(cluster.getId(), new ArrayList<>(hostCpuMap.values()), new ArrayList<>(hostMemoryMap.values()))) {
            Pair<VirtualMachine, Host> bestMigration = getBestMigration(cluster, algorithm, vmList, vmIdServiceOfferingMap, hostCpuMap, hostMemoryMap);
            VirtualMachine vm = bestMigration.first();
            Host destHost = bestMigration.second();
            if (destHost == null || vm == null || originalHostIdVmIdMap.get(destHost.getId()).contains(vm.getId())) {
                logger.debug("VM migrating to it's original host or no host found for migration");
                break;
            }

            ServiceOffering serviceOffering = vmIdServiceOfferingMap.get(vm.getId());
            migrationPlan.add(new Ternary<>(vm, hostMap.get(vm.getHostId()), hostMap.get(destHost.getId())));

            hostVmMap.get(vm.getHostId()).remove(vm);
            hostVmMap.get(destHost.getId()).add(vm);
            hostVmMap.get(vm.getHostId()).remove(vm);
            hostVmMap.get(destHost.getId()).add(vm);

            long vmCpu = (long) serviceOffering.getCpu() * serviceOffering.getSpeed();
            long vmMemory = serviceOffering.getRamSize() * 1024L * 1024L;

            hostCpuMap.put(vm.getHostId(), hostCpuMap.get(vm.getHostId()) - vmCpu);
            hostCpuMap.put(destHost.getId(), hostCpuMap.get(destHost.getId()) + vmCpu);
            hostMemoryMap.put(vm.getHostId(), hostMemoryMap.get(vm.getHostId()) - vmMemory);
            hostMemoryMap.put(destHost.getId(), hostMemoryMap.get(destHost.getId()) + vmMemory);
            vm.setHostId(destHost.getId());
            iteration++;
        }
        return migrationPlan;
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
     * Returns the best migration for a given cluster using the specified DRS algorithm.
     *
     * @param cluster                the cluster to perform DRS on
     * @param algorithm              the DRS algorithm to use
     * @param vmList                 the list of virtual machines to consider for migration
     * @param vmIdServiceOfferingMap a map of virtual machine IDs to their corresponding service offerings
     * @param hostCpuCapacityMap     a map of host IDs to their corresponding CPU capacity
     * @param hostMemoryCapacityMap  a map of host IDs to their corresponding memory capacity
     * @return a pair of the virtual machine and host that represent the best migration, or null if no migration is possible
     */
    Pair<VirtualMachine, Host> getBestMigration(
            Cluster cluster,
            ClusterDrsAlgorithm algorithm,
            List<VirtualMachine> vmList,
            Map<Long, ServiceOffering> vmIdServiceOfferingMap,
            Map<Long, Long> hostCpuCapacityMap,
            Map<Long, Long> hostMemoryCapacityMap
    ) {
        double maxImprovement = 0;
        Pair<VirtualMachine, Host> bestMigration = new Pair<>(null, null);

        for (VirtualMachine vm : vmList) {
            if (vm.getType().isUsedBySystem()) {
                continue;
            }
            if (MapUtils.isNotEmpty(vm.getDetails()) && vm.getDetails().get(VmDetailConstants.SKIP_DRS).equalsIgnoreCase("true")) {
                continue;
            }
            Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>> hostsForMigrationOfVM = managementServer.listHostsForMigrationOfVM(vm, 0L, null, null, vmList);
            List<? extends Host> compatibleDestinationHosts = hostsForMigrationOfVM.second();
            Map<Host, Boolean> requiresStorageMotion = hostsForMigrationOfVM.third();

            for (Host destHost : compatibleDestinationHosts) {
                Ternary<Double, Double, Double> metrics = algorithm.getMetrics(cluster.getId(), vm, vmIdServiceOfferingMap.get(vm.getId()), destHost, hostCpuCapacityMap, hostMemoryCapacityMap, requiresStorageMotion.get(destHost));

                Double improvement = metrics.first();
                Double cost = metrics.second();
                Double benefit = metrics.third();
                if (benefit > cost && (improvement > maxImprovement)) {
                    bestMigration = new Pair<>(vm, destHost);
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
            if (cluster.getAllocationState() == Disabled || cluster.getClusterType() != Cluster.ClusterType.CloudManaged || ClusterDrsEnabled.valueIn(cluster.getId()).equals(Boolean.FALSE)
                    || drsEventsDao.lastAutomatedDrsEventInInterval(cluster.getId(), ClusterDrsInterval.valueIn(cluster.getId())) != null) {
                continue;
            }
            Long jobId = null;
            long eventId = ActionEventUtils.onStartedActionEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_CLUSTER_DRS,
                    String.format("Executing automated DRS for cluster %s", cluster.getUuid()), cluster.getId(), ApiCommandResourceType.Cluster.toString(), true, 0);
            CallContext.current().setStartEventId(eventId);
            try {
                List<Ternary<VirtualMachine, Host, Host>> plan = getDrsPlan(cluster, ClusterDrsIterations.valueIn(cluster.getId()));
                jobId = executeDrsPlan(cluster.getId(), plan);
                logger.info(String.format("Executed DRS on cluster %s [id=%s] with job id %d", cluster.getName(), cluster.getUuid(), jobId));
                drsEventsDao.persist(new ClusterDrsEventsVO(cluster.getId(), eventId, new Date(), jobId, ClusterDrsEvents.Type.AUTOMATED, ClusterDrsEvents.Result.SUCCESS));

                ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventVO.LEVEL_INFO, EventTypes.EVENT_CLUSTER_DRS, true,
                        String.format("Created async job [id=%d] for automatic DRS of cluster %s", jobId, cluster.getName()), cluster.getId(), ApiCommandResourceType.Cluster.toString(), eventId);
            } catch (Exception e) {
                drsEventsDao.persist(new ClusterDrsEventsVO(cluster.getId(), eventId, new Date(), jobId, ClusterDrsEvents.Type.AUTOMATED, ClusterDrsEvents.Result.FAILURE));
                logger.error(String.format("Unable to execute DRS on cluster %s [id=%s]", cluster.getName(), cluster.getUuid()), e);
            }
        }
    }
}
