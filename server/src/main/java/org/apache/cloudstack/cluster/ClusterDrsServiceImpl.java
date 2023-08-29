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
import com.cloud.domain.Domain;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
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
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.cluster.ExecuteClusterDrsPlanCmd;
import org.apache.cloudstack.api.command.admin.cluster.GenerateClusterDrsPlanCmd;
import org.apache.cloudstack.api.command.admin.cluster.ListClusterDrsPlanCmd;
import org.apache.cloudstack.api.command.admin.vm.MigrateVMCmd;
import org.apache.cloudstack.api.response.ClusterDrsPlanMigrationResponse;
import org.apache.cloudstack.api.response.ClusterDrsPlanResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.cluster.dao.ClusterDrsPlanDao;
import org.apache.cloudstack.cluster.dao.ClusterDrsPlanMigrationDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.jobs.JobInfo;
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

public class ClusterDrsServiceImpl extends ManagerBase implements ClusterDrsService, PluggableService {

    private static final Logger logger = Logger.getLogger(ClusterDrsServiceImpl.class);

    private static final String CLUSTER_LOCK_STR = "drs.plan.cluster.%s";

    AsyncJobDispatcher asyncJobDispatcher;

    @Inject
    AsyncJobManager asyncJobManager;

    @Inject
    ClusterDao clusterDao;

    @Inject
    HostDao hostDao;

    @Inject
    EventDao eventDao;

    @Inject
    HostJoinDao hostJoinDao;

    @Inject
    VMInstanceDao vmInstanceDao;

    @Inject
    ClusterDrsPlanDao drsPlanDao;

    @Inject
    ClusterDrsPlanMigrationDao drsPlanMigrationDao;

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
                    updateOldPlanMigrations();
                    // Executing processPlans() twice to update the migration status of plans which
                    // are completed and
                    // if required generate new plans.
                    processPlans();
                    generateDrsPlanForAllClusters();
                    processPlans();
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
                    cleanUpOldDrsPlans();
                } finally {
                    cleanupLock.unlock();
                }
            }
        } finally {
            cleanupLock.releaseRef();
        }
    }

    /**
     * Fetches the plans which are in progress and updates their migration status.
     */
    void updateOldPlanMigrations() {
        List<ClusterDrsPlanVO> plans = drsPlanDao.listByStatus(ClusterDrsPlan.Status.IN_PROGRESS);
        for (ClusterDrsPlanVO plan : plans) {
            try {
                updateDrsPlanMigrations(plan);
            } catch (Exception e) {
                logger.error(String.format("Unable to update DRS plan details [id=%d]", plan.getId()), e);
            }
        }
    }

    /**
     * Updates the job status of the plan details for the given plan.
     *
     * @param plan
     *         the plan to update
     */
    void updateDrsPlanMigrations(ClusterDrsPlanVO plan) {
        List<ClusterDrsPlanMigrationVO> migrations = drsPlanMigrationDao.listPlanMigrationsInProgress(plan.getId());
        if (migrations == null || migrations.isEmpty()) {
            plan.setStatus(ClusterDrsPlan.Status.COMPLETED);
            drsPlanDao.update(plan.getId(), plan);
            ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventVO.LEVEL_INFO,
                    EventTypes.EVENT_CLUSTER_DRS, true,
                    String.format("DRS execution task completed for cluster [id=%s]", plan.getClusterId()),
                    plan.getClusterId(), ApiCommandResourceType.Cluster.toString(), plan.getEventId());
            return;
        }

        for (ClusterDrsPlanMigrationVO migration : migrations) {
            try {
                AsyncJobVO job = asyncJobManager.getAsyncJob(migration.getJobId());
                if (job == null) {
                    logger.warn(String.format("Unable to find async job [id=%d] for DRS plan migration [id=%d]",
                            migration.getJobId(), migration.getId()));
                    migration.setStatus(JobInfo.Status.FAILED);
                    drsPlanMigrationDao.update(migration.getId(), migration);
                    continue;
                }
                if (job.getStatus() != JobInfo.Status.IN_PROGRESS) {
                    migration.setStatus(job.getStatus());
                    drsPlanMigrationDao.update(migration.getId(), migration);
                }
            } catch (Exception e) {
                logger.error(String.format("Unable to update DRS plan migration [id=%d]", migration.getId()), e);
            }
        }
    }

    /**
     * Generates DRS for all clusters that meet the criteria for automated DRS.
     */
    void generateDrsPlanForAllClusters() {
        List<ClusterVO> clusterList = clusterDao.listAll();

        for (ClusterVO cluster : clusterList) {
            if (cluster.getAllocationState() == Disabled || ClusterDrsEnabled.valueIn(
                    cluster.getId()).equals(Boolean.FALSE)) {
                continue;
            }

            ClusterDrsPlanVO lastPlan = drsPlanDao.listLatestPlanForClusterId(cluster.getId());

            // If the last plan is ready or in progress or was executed within the last interval, skip this cluster.
            // This is to avoid generating plans for clusters which are already being processed and to avoid
            // generating plans for clusters which have been processed recently.This doesn't consider the type
            // (manual or automated) of the last plan.
            if (lastPlan != null && (lastPlan.getStatus() == ClusterDrsPlan.Status.READY ||
                    lastPlan.getStatus() == ClusterDrsPlan.Status.IN_PROGRESS ||
                    (lastPlan.getStatus() == ClusterDrsPlan.Status.COMPLETED &&
                            lastPlan.getCreated().compareTo(DateUtils.addMinutes(new Date(), -1 * ClusterDrsInterval.valueIn(cluster.getId()))) > 0)
            )) {
                continue;
            }

            long eventId = ActionEventUtils.onStartedActionEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM,
                    EventTypes.EVENT_CLUSTER_DRS,
                    String.format("Generating DRS plan for cluster %s", cluster.getUuid()), cluster.getId(),
                    ApiCommandResourceType.Cluster.toString(), true, 0);
            GlobalLock clusterLock = GlobalLock.getInternLock(String.format(CLUSTER_LOCK_STR, cluster.getId()));
            try {
                if (clusterLock.lock(30)) {
                    try {
                        List<Ternary<VirtualMachine, Host, Host>> plan = getDrsPlan(cluster,
                                ClusterDrsMaxMigrations.valueIn(cluster.getId()));
                        savePlan(cluster.getId(), plan, eventId, ClusterDrsPlan.Type.AUTOMATED,
                                ClusterDrsPlan.Status.READY);
                        logger.info(String.format("Generated DRS plan for cluster %s [id=%s]", cluster.getName(),
                                cluster.getUuid()));
                    } catch (Exception e) {
                        logger.error(
                                String.format("Unable to generate DRS plans for cluster %s [id=%s]", cluster.getName(),
                                        cluster.getUuid()),
                                e);
                    } finally {
                        clusterLock.unlock();
                    }
                }
            } finally {
                clusterLock.releaseRef();
            }
        }
    }

    /**
     * Generate DRS plan for the given cluster with the specified iteration percentage.
     *
     * @param cluster
     *         The cluster to generate DRS for.
     * @param maxIterations
     *         The percentage of VMs to consider for migration
     *         during each iteration. Value between 0 and 1.
     *
     * @return List of Ternary object containing VM to be migrated, source host and
     *         destination host.
     *
     * @throws ConfigurationException
     *         If there is an error in the DRS configuration.
     */
    List<Ternary<VirtualMachine, Host, Host>> getDrsPlan(Cluster cluster,
            int maxIterations) throws ConfigurationException {
        List<Ternary<VirtualMachine, Host, Host>> migrationPlan = new ArrayList<>();

        if (cluster.getAllocationState() == Disabled || maxIterations <= 0) {
            return Collections.emptyList();
        }
        ClusterDrsAlgorithm algorithm = getDrsAlgorithm(ClusterDrsAlgorithm.valueIn(cluster.getId()));
        List<HostVO> hostList = hostDao.findByClusterId(cluster.getId());
        List<VirtualMachine> vmList = new ArrayList<>(vmInstanceDao.listByClusterId(cluster.getId()));

        int iteration = 0;

        Map<Long, Host> hostMap = hostList.stream().collect(Collectors.toMap(HostVO::getId, host -> host));

        Map<Long, List<VirtualMachine>> hostVmMap = getHostVmMap(hostList, vmList);
        Map<Long, List<Long>> originalHostIdVmIdMap = new HashMap<>();
        for (HostVO host : hostList) {
            originalHostIdVmIdMap.put(host.getId(), new ArrayList<>());
            for (VirtualMachine vm : hostVmMap.get(host.getId())) {
                originalHostIdVmIdMap.get(host.getId()).add(vm.getId());
            }
        }

        List<HostJoinVO> hostJoinList = hostJoinDao.searchByIds(
                hostList.stream().map(HostVO::getId).toArray(Long[]::new));

        Map<Long, Long> hostCpuMap = hostJoinList.stream().collect(Collectors.toMap(HostJoinVO::getId,
                hostJoin -> hostJoin.getCpuUsedCapacity() + hostJoin.getCpuReservedCapacity()));
        Map<Long, Long> hostMemoryMap = hostJoinList.stream().collect(Collectors.toMap(HostJoinVO::getId,
                hostJoin -> hostJoin.getMemUsedCapacity() + hostJoin.getMemReservedCapacity()));

        Map<Long, ServiceOffering> vmIdServiceOfferingMap = new HashMap<>();

        for (VirtualMachine vm : vmList) {
            vmIdServiceOfferingMap.put(vm.getId(),
                    serviceOfferingDao.findByIdIncludingRemoved(vm.getId(), vm.getServiceOfferingId()));
        }

        while (iteration < maxIterations && algorithm.needsDrs(cluster.getId(), new ArrayList<>(hostCpuMap.values()),
                new ArrayList<>(hostMemoryMap.values()))) {
            Pair<VirtualMachine, Host> bestMigration = getBestMigration(cluster, algorithm, vmList,
                    vmIdServiceOfferingMap, hostCpuMap, hostMemoryMap);
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

    private ClusterDrsAlgorithm getDrsAlgorithm(String algoName) {
        if (drsAlgorithmMap.containsKey(algoName)) {
            return drsAlgorithmMap.get(algoName);
        }
        throw new CloudRuntimeException("Invalid algorithm configured!");
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
     * Returns the best migration for a given cluster using the specified DRS
     * algorithm.
     *
     * @param cluster
     *         the cluster to perform DRS on
     * @param algorithm
     *         the DRS algorithm to use
     * @param vmList
     *         the list of virtual machines to consider for
     *         migration
     * @param vmIdServiceOfferingMap
     *         a map of virtual machine IDs to their
     *         corresponding service offerings
     * @param hostCpuCapacityMap
     *         a map of host IDs to their corresponding CPU
     *         capacity
     * @param hostMemoryCapacityMap
     *         a map of host IDs to their corresponding memory
     *         capacity
     *
     * @return a pair of the virtual machine and host that represent the best
     *         migration, or null if no migration is
     *         possible
     */
    Pair<VirtualMachine, Host> getBestMigration(Cluster cluster, ClusterDrsAlgorithm algorithm,
            List<VirtualMachine> vmList,
            Map<Long, ServiceOffering> vmIdServiceOfferingMap,
            Map<Long, Long> hostCpuCapacityMap,
            Map<Long, Long> hostMemoryCapacityMap) {
        double improvement = 0;
        Pair<VirtualMachine, Host> bestMigration = new Pair<>(null, null);

        for (VirtualMachine vm : vmList) {
            if (vm.getType().isUsedBySystem() || vm.getState() != VirtualMachine.State.Running ||
                    (MapUtils.isNotEmpty(vm.getDetails()) &&
                            vm.getDetails().get(VmDetailConstants.SKIP_DRS).equalsIgnoreCase("true"))
            ) {
                continue;
            }
            Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>> hostsForMigrationOfVM = managementServer
                    .listHostsForMigrationOfVM(
                            vm, 0L, 500L, null, vmList);
            List<? extends Host> compatibleDestinationHosts = hostsForMigrationOfVM.first().first();
            List<? extends Host> suitableDestinationHosts = hostsForMigrationOfVM.second();

            Map<Host, Boolean> requiresStorageMotion = hostsForMigrationOfVM.third();

            for (Host destHost : compatibleDestinationHosts) {
                if (!suitableDestinationHosts.contains(destHost)) {
                    continue;
                }
                Ternary<Double, Double, Double> metrics = algorithm.getMetrics(cluster.getId(), vm,
                        vmIdServiceOfferingMap.get(vm.getId()), destHost, hostCpuCapacityMap, hostMemoryCapacityMap,
                        requiresStorageMotion.get(destHost));

                Double currentImprovement = metrics.first();
                Double cost = metrics.second();
                Double benefit = metrics.third();
                if (benefit > cost && (currentImprovement > improvement)) {
                    bestMigration = new Pair<>(vm, destHost);
                    improvement = currentImprovement;
                }
            }
        }
        return bestMigration;
    }


    /**
     * Saves a DRS plan for a given cluster and returns the saved plan along with the list of migrations to be executed.
     *
     * @param clusterId
     *         the ID of the cluster for which the DRS plan is being saved
     * @param plan
     *         the list of virtual machine migrations to be executed as part of the DRS plan
     * @param eventId
     *         the ID of the event that triggered the DRS plan
     * @param type
     *         the type of the DRS plan
     *
     * @return a pair of the saved DRS plan and the list of migrations to be executed
     */
    Pair<ClusterDrsPlanVO, List<ClusterDrsPlanMigrationVO>> savePlan(Long clusterId,
            List<Ternary<VirtualMachine, Host, Host>> plan,
            Long eventId, ClusterDrsPlan.Type type,
            ClusterDrsPlan.Status status) {
        return Transaction.execute(
                (TransactionCallback<Pair<ClusterDrsPlanVO, List<ClusterDrsPlanMigrationVO>>>) txStatus -> {
                    ClusterDrsPlanVO drsPlan = drsPlanDao.persist(
                            new ClusterDrsPlanVO(clusterId, eventId, type, status));
                    List<ClusterDrsPlanMigrationVO> planMigrations = new ArrayList<>();
                    for (Ternary<VirtualMachine, Host, Host> migration : plan) {
                        VirtualMachine vm = migration.first();
                        Host srcHost = migration.second();
                        Host destHost = migration.third();
                        planMigrations.add(drsPlanMigrationDao.persist(
                                new ClusterDrsPlanMigrationVO(drsPlan.getId(), vm.getId(), srcHost.getId(),
                                        destHost.getId())));
                    }
                    return new Pair<>(drsPlan, planMigrations);
                });
    }

    /**
     * Processes all DRS plans that are in the READY status.
     */
    void processPlans() {
        List<ClusterDrsPlanVO> plans = drsPlanDao.listByStatus(ClusterDrsPlan.Status.READY);
        for (ClusterDrsPlanVO plan : plans) {
            try {
                executeDrsPlan(plan);
            } catch (Exception e) {
                logger.error(String.format("Unable to execute DRS plan [id=%d]", plan.getId()), e);
            }
        }
    }

    /**
     * Executes the DRS plan by migrating virtual machines to their destination hosts.
     * If there are no migrations to be executed, the plan is marked as completed.
     *
     * @param plan
     *         the DRS plan to be executed
     */
    void executeDrsPlan(ClusterDrsPlanVO plan) {
        List<ClusterDrsPlanMigrationVO> planMigrations = drsPlanMigrationDao.listPlanMigrationsToExecute(plan.getId());
        if (planMigrations == null || planMigrations.isEmpty()) {
            plan.setStatus(ClusterDrsPlan.Status.COMPLETED);
            drsPlanDao.update(plan.getId(), plan);
            ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventVO.LEVEL_INFO,
                    EventTypes.EVENT_CLUSTER_DRS, true,
                    String.format("DRS execution task completed for cluster [id=%s]", plan.getClusterId()),
                    plan.getClusterId(), ApiCommandResourceType.Cluster.toString(), plan.getEventId());
            return;
        }

        plan.setStatus(ClusterDrsPlan.Status.IN_PROGRESS);
        drsPlanDao.update(plan.getId(), plan);

        for (ClusterDrsPlanMigrationVO migration : planMigrations) {
            try {
                VirtualMachine vm = vmInstanceDao.findById(migration.getVmId());
                Host host = hostDao.findById(migration.getDestHostId());
                if (vm == null || host == null) {
                    throw new CloudRuntimeException(String.format("vm %s or host %s is not found", migration.getVmId(),
                            migration.getDestHostId()));
                }

                logger.debug(
                        String.format("Executing DRS plan %s for vm %s to host %s", plan.getId(), vm.getInstanceName(),
                                host.getName()));
                long jobId = createMigrateVMAsyncJob(vm, host, plan.getEventId());
                AsyncJobVO job = asyncJobManager.getAsyncJob(jobId);
                migration.setJobId(jobId);
                migration.setStatus(job.getStatus());
                drsPlanMigrationDao.update(migration.getId(), migration);
            } catch (Exception e) {
                logger.warn(String.format("Unable to execute DRS plan %s due to %s", plan.getUuid(), e.getMessage()));
                migration.setStatus(JobInfo.Status.FAILED);
                drsPlanMigrationDao.update(migration.getId(), migration);
            }
        }
    }

    /**
     * Creates an asynchronous job to migrate a virtual machine to a specified host.
     *
     * @param vm
     *         the virtual machine to be migrated
     * @param host
     *         the destination host for the virtual machine
     * @param eventId
     *         the ID of the event that triggered the migration
     *
     * @return the ID of the created asynchronous job
     */
    long createMigrateVMAsyncJob(VirtualMachine vm, Host host, long eventId) {
        final Map<String, String> params = new HashMap<>();
        params.put("ctxUserId", String.valueOf(User.UID_SYSTEM));
        params.put("ctxAccountId", String.valueOf(Account.ACCOUNT_ID_SYSTEM));
        params.put(ApiConstants.CTX_START_EVENT_ID, String.valueOf(eventId));
        params.put(ApiConstants.HOST_ID, String.valueOf(host.getId()));
        params.put(ApiConstants.VIRTUAL_MACHINE_ID, String.valueOf(vm.getId()));

        final MigrateVMCmd cmd = new MigrateVMCmd();
        ComponentContext.inject(cmd);

        AsyncJobVO job = new AsyncJobVO("", User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, MigrateVMCmd.class.getName(),
                ApiGsonHelper.getBuilder().create().toJson(params), vm.getId(),
                ApiCommandResourceType.VirtualMachine.toString(), null);
        job.setDispatcher(asyncJobDispatcher.getName());

        return asyncJobManager.submitAsyncJob(job);
    }

    /**
     * Removes old DRS migrations records that have expired based on the configured interval.
     */
    void cleanUpOldDrsPlans() {
        Date date = DateUtils.addDays(new Date(), -1 * ClusterDrsPlanExpireInterval.value());
        int rowsRemoved = drsPlanDao.expungeBeforeDate(date);
        logger.debug(String.format("Removed %d old drs migration plans", rowsRemoved));
    }

    @Override
    public String getConfigComponentName() {
        return ClusterDrsService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{ClusterDrsPlanExpireInterval, ClusterDrsEnabled, ClusterDrsInterval,
                ClusterDrsMaxMigrations, ClusterDrsAlgorithm, ClusterDrsImbalanceThreshold, ClusterDrsMetric};
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(ListClusterDrsPlanCmd.class);
        cmdList.add(GenerateClusterDrsPlanCmd.class);
        cmdList.add(ExecuteClusterDrsPlanCmd.class);
        return cmdList;
    }

    /**
     * Generates a DRS plan for the given cluster and returns a list of migration responses.
     *
     * @param cmd
     *         the command containing the cluster ID and number of migrations for the DRS plan
     *
     * @return a list response of migration responses for the generated DRS plan
     *
     * @throws InvalidParameterValueException
     *         if the cluster is not found, is disabled, or is not a cloud stack managed cluster, or if the number of
     *         migrations is invalid
     * @throws CloudRuntimeException
     *         if there is an error scheduling the DRS plan
     */
    @Override
    public ClusterDrsPlanResponse generateDrsPlan(GenerateClusterDrsPlanCmd cmd) {
        Cluster cluster = clusterDao.findById(cmd.getId());
        if (cluster == null) {
            throw new InvalidParameterValueException("Unable to find the cluster by id=" + cmd.getId());
        }
        if (cluster.getAllocationState() == Disabled) {
            throw new InvalidParameterValueException(
                    String.format("Unable to execute DRS on the cluster %s as it is disabled", cluster.getName()));
        }
        if (cmd.getMaxMigrations() <= 0) {
            throw new InvalidParameterValueException(
                    String.format("Unable to execute DRS on the cluster %s as the number of migrations [%s] is invalid",
                            cluster.getName(), cmd.getMaxMigrations()));
        }

        try {
            List<Ternary<VirtualMachine, Host, Host>> plan = getDrsPlan(cluster, cmd.getMaxMigrations());
            long eventId = ActionEventUtils.onActionEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM,
                    Domain.ROOT_DOMAIN,
                    EventTypes.EVENT_CLUSTER_DRS_GENERATE,
                    String.format("Generating DRS plan for cluster %s", cluster.getUuid()), cluster.getId(),
                    ApiCommandResourceType.Cluster.toString());
            List<ClusterDrsPlanMigrationVO> migrations;
            ClusterDrsPlanVO drsPlan = new ClusterDrsPlanVO(
                    cluster.getId(), eventId, ClusterDrsPlan.Type.MANUAL, ClusterDrsPlan.Status.UNDER_REVIEW);
            migrations = new ArrayList<>();
            for (Ternary<VirtualMachine, Host, Host> migration : plan) {
                VirtualMachine vm = migration.first();
                Host srcHost = migration.second();
                Host destHost = migration.third();
                migrations.add(new ClusterDrsPlanMigrationVO(0L, vm.getId(), srcHost.getId(), destHost.getId()));
            }

            CallContext.current().setEventResourceType(ApiCommandResourceType.Cluster);
            CallContext.current().setEventResourceId(cluster.getId());

            String eventUuid = null;
            EventVO event = eventDao.findById(drsPlan.getEventId());
            if (event != null) {
                eventUuid = event.getUuid();
            }

            return new ClusterDrsPlanResponse(
                    cluster.getUuid(), drsPlan, eventUuid, getResponseObjectForMigrations(migrations));
        } catch (ConfigurationException e) {
            throw new CloudRuntimeException("Unable to schedule DRS", e);
        }
    }

    /**
     * Returns a list of ClusterDrsPlanMigrationResponse objects for the given list of ClusterDrsPlanMigrationVO
     * objects.
     *
     * @param migrations
     *         the list of ClusterDrsPlanMigrationVO objects
     *
     * @return a list of ClusterDrsPlanMigrationResponse objects
     */
    List<ClusterDrsPlanMigrationResponse> getResponseObjectForMigrations(List<ClusterDrsPlanMigrationVO> migrations) {
        if (migrations == null) {
            return Collections.emptyList();
        }
        List<ClusterDrsPlanMigrationResponse> responses = new ArrayList<>();

        for (ClusterDrsPlanMigrationVO migration : migrations) {
            VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(migration.getVmId());
            HostVO srcHost = hostDao.findByIdIncludingRemoved(migration.getSrcHostId());
            HostVO destHost = hostDao.findByIdIncludingRemoved(migration.getDestHostId());
            responses.add(new ClusterDrsPlanMigrationResponse(
                    vm.getUuid(), vm.getInstanceName(),
                    srcHost.getUuid(), srcHost.getName(),
                    destHost.getUuid(), destHost.getName(),
                    migration.getJobId(), migration.getStatus()));
        }

        return responses;
    }

    @Override
    public ClusterDrsPlanResponse executeDrsPlan(ExecuteClusterDrsPlanCmd cmd) {

        Map<VirtualMachine, Host> vmToHostMap = cmd.getVmToHostMap();
        Long clusterId = cmd.getId();

        if (vmToHostMap.isEmpty()) {
            throw new InvalidParameterValueException("migrateto can not be empty.");
        }

        Cluster cluster = clusterDao.findById(clusterId);

        if (cluster == null) {
            throw new InvalidParameterValueException("cluster not found");
        }

        return executeDrsPlan(cluster, vmToHostMap);

    }

    private ClusterDrsPlanResponse executeDrsPlan(Cluster cluster, Map<VirtualMachine, Host> vmToHostMap) {
        // To ensure that no other plan is generated for this cluster, we take a lock
        GlobalLock clusterLock = GlobalLock.getInternLock(String.format(CLUSTER_LOCK_STR, cluster.getId()));
        ClusterDrsPlanVO drsPlan = null;
        List<ClusterDrsPlanMigrationVO> migrations = null;
        try {
            if (clusterLock.lock(5)) {
                try {
                    List<ClusterDrsPlanVO> readyPlans = drsPlanDao.listByClusterIdAndStatus(cluster.getId(),
                            ClusterDrsPlan.Status.READY);
                    if (readyPlans != null && !readyPlans.isEmpty()) {
                        throw new InvalidParameterValueException(
                                String.format(
                                        "Unable to execute DRS plan as there is already a plan [id=%s] in READY state",
                                        readyPlans.get(0).getUuid()));
                    }
                    List<ClusterDrsPlanVO> inProgressPlans = drsPlanDao.listByClusterIdAndStatus(cluster.getId(),
                            ClusterDrsPlan.Status.IN_PROGRESS);

                    if (inProgressPlans != null && !inProgressPlans.isEmpty()) {
                        throw new InvalidParameterValueException(
                                String.format("Unable to execute DRS plan as there is already a plan [id=%s] in In " +
                                                "Progress",
                                        inProgressPlans.get(0).getUuid()));
                    }

                    List<Ternary<VirtualMachine, Host, Host>> plan = new ArrayList<>();
                    for (Map.Entry<VirtualMachine, Host> entry : vmToHostMap.entrySet()) {
                        VirtualMachine vm = entry.getKey();
                        Host destHost = entry.getValue();
                        Host srcHost = hostDao.findById(vm.getHostId());
                        plan.add(new Ternary<>(vm, srcHost, destHost));
                    }

                    Pair<ClusterDrsPlanVO, List<ClusterDrsPlanMigrationVO>> pair = savePlan(cluster.getId(), plan,
                            CallContext.current().getStartEventId(), ClusterDrsPlan.Type.MANUAL,
                            ClusterDrsPlan.Status.READY);
                    drsPlan = pair.first();
                    migrations = pair.second();

                    executeDrsPlan(drsPlan);
                } finally {
                    clusterLock.unlock();
                }
            }
        } finally {
            clusterLock.releaseRef();
        }

        String eventId = null;
        if (drsPlan != null) {
            EventVO event = eventDao.findById(drsPlan.getEventId());
            eventId = event.getUuid();
        }

        return new ClusterDrsPlanResponse(
                cluster.getUuid(), drsPlan, eventId, getResponseObjectForMigrations(migrations));
    }

    @Override
    public ListResponse<ClusterDrsPlanResponse> listDrsPlan(ListClusterDrsPlanCmd cmd) {
        Long clusterId = cmd.getClusterId();
        Long planId = cmd.getId();

        if (planId != null && clusterId != null) {
            throw new InvalidParameterValueException("Only one of clusterId or planId can be specified");
        }

        ClusterVO cluster = clusterDao.findById(clusterId);
        if (clusterId != null && cluster == null) {
            throw new InvalidParameterValueException("Unable to find the cluster by id=" + clusterId);
        }

        Pair<List<ClusterDrsPlanVO>, Integer> result = drsPlanDao.searchAndCount(clusterId, planId, cmd.getStartIndex(),
                cmd.getPageSizeVal());

        ListResponse<ClusterDrsPlanResponse> response = new ListResponse<>();
        List<ClusterDrsPlanResponse> responseList = new ArrayList<>();

        for (ClusterDrsPlan plan : result.first()) {
            if (cluster == null || plan.getClusterId() != cluster.getId()) {
                cluster = clusterDao.findById(plan.getClusterId());
            }
            List<ClusterDrsPlanMigrationVO> migrations = drsPlanMigrationDao.listByPlanId(plan.getId());
            EventVO event = eventDao.findById(plan.getEventId());

            responseList.add(new ClusterDrsPlanResponse(
                    cluster.getUuid(), plan, event.getUuid(), getResponseObjectForMigrations(migrations)));
        }

        response.setResponses(responseList, result.second());
        return response;
    }
}
