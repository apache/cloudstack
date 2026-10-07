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
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.domain.Domain;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.resource.ResourceState;
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
import com.cloud.vm.VMInstanceDetailVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagement;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagementService;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfileImpl;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.VMInstanceDetailsDao;
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
import org.apache.cloudstack.affinity.AffinityGroupVMMapVO;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.apache.cloudstack.cluster.dao.ClusterDrsPlanDao;
import org.apache.cloudstack.cluster.dao.ClusterDrsPlanMigrationDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.jobs.JobInfo;
import org.apache.cloudstack.managed.context.ManagedContextTimerTask;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import static com.cloud.org.Grouping.AllocationState.Disabled;
import static org.apache.cloudstack.cluster.ClusterDrsAlgorithm.getClusterImbalance;
import static org.apache.cloudstack.cluster.ClusterDrsAlgorithm.getClusterDrsMetric;
import static org.apache.cloudstack.cluster.ClusterDrsAlgorithm.getMetricValue;

public class ClusterDrsServiceImpl extends ManagerBase implements ClusterDrsService, PluggableService {

    private static final String CLUSTER_LOCK_STR = "drs.plan.cluster.%s";

    AsyncJobDispatcher asyncJobDispatcher;

    @Inject
    AsyncJobManager asyncJobManager;

    @Inject
    ClusterDao clusterDao;

    @Inject
    HostDao hostDao;

    @Inject
    HostDetailsDao hostDetailsDao;

    @Inject
    OutOfBandManagementService outOfBandManagementService;

    @Inject
    EventDao eventDao;

    // Host detail marking a host that DRS power management powered off, so only those are powered back on
    // (never a host that is down for another reason).
    protected static final String DRS_POWER_STATE_DETAIL = "drs.power.state";
    protected static final String DRS_POWER_STATE_OFF = "off";
    // Marks a host DRS is actively draining so it can be powered off once empty; distinct from OFF so a draining
    // host is never treated as a wake candidate and is picked up across polls to continue or abandon the drain.
    protected static final String DRS_POWER_STATE_DRAINING = "draining";

    // Recent cluster utilization samples (ratio on the DRS metric) per cluster, newest last, used by predictive DRS.
    // Written only from the single-threaded poll under the clusterDRS.poll lock.
    protected final Map<Long, LinkedList<Double>> clusterUtilizationHistory = new ConcurrentHashMap<>();

    // VM count on each host at the last drain batch, so a drain that stops making progress (migrations rejected for
    // affinity/tags/storage) is abandoned instead of re-submitted forever. Written only under the poll lock.
    protected final Map<Long, Integer> drainingVmCountByHost = new ConcurrentHashMap<>();

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
    VMInstanceDetailsDao vmInstanceDetailsDao;

    @Inject
    ManagementServer managementServer;

    @Inject
    AffinityGroupVMMapDao affinityGroupVMMapDao;

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
                    managePowerForAllClusters();
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
                logger.error("Unable to update DRS plan details {}", plan, e);
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
                    String.format("DRS execution task completed for cluster %s", clusterDao.findById(plan.getClusterId())),
                    plan.getClusterId(), ApiCommandResourceType.Cluster.toString(), plan.getEventId());
            return;
        }

        for (ClusterDrsPlanMigrationVO migration : migrations) {
            try {
                AsyncJobVO job = asyncJobManager.getAsyncJob(migration.getJobId());
                if (job == null) {
                    logger.warn("Unable to find async job [id={}] for DRS plan migration {}", migration.getJobId(), migration);
                    migration.setStatus(JobInfo.Status.FAILED);
                    drsPlanMigrationDao.update(migration.getId(), migration);
                    continue;
                }
                if (job.getStatus() != JobInfo.Status.IN_PROGRESS) {
                    migration.setStatus(job.getStatus());
                    drsPlanMigrationDao.update(migration.getId(), migration);
                }
            } catch (Exception e) {
                logger.error("Unable to update DRS plan migration {}", migration, e);
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
                        logger.info("Generated DRS plan for cluster {}", cluster);
                    } catch (Exception e) {
                        logger.error("Unable to generate DRS plans for cluster {}", cluster, e);
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
    List<Ternary<VirtualMachine, Host, Host>> getDrsPlan(Cluster cluster, int maxIterations) throws ConfigurationException {

        if (cluster.getAllocationState() == Disabled || maxIterations <= 0) {
            return Collections.emptyList();
        }
        List<HostVO> hostList = hostDao.findByClusterId(cluster.getId());
        List<VirtualMachine> vmList = new ArrayList<>(vmInstanceDao.listByClusterId(cluster.getId()));

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

        Map<Long, Ternary<Long, Long, Long>> hostCpuMap = hostJoinList.stream().collect(Collectors.toMap(HostJoinVO::getId,
                hostJoin -> new Ternary<>(hostJoin.getCpuUsedCapacity(), hostJoin.getCpuReservedCapacity(), hostJoin.getCpus() * hostJoin.getSpeed())));
        Map<Long, Ternary<Long, Long, Long>> hostMemoryMap = hostJoinList.stream().collect(Collectors.toMap(HostJoinVO::getId,
                hostJoin -> new Ternary<>(hostJoin.getMemUsedCapacity(), hostJoin.getMemReservedCapacity(), hostJoin.getTotalMemory())));

        Map<Long, ServiceOffering> vmIdServiceOfferingMap = new HashMap<>();

        for (VirtualMachine vm : vmList) {
            vmIdServiceOfferingMap.put(vm.getId(),
                    serviceOfferingDao.findByIdIncludingRemoved(vm.getId(), vm.getServiceOfferingId()));
        }

        Pair<Map<Long, List<? extends Host>>, Map<Long, Map<Host, Boolean>>> hostCache = getCompatibleHostAndVmStorageMotionCache(vmList);
        Map<Long, List<? extends Host>> vmToCompatibleHostsCache = hostCache.first();
        Map<Long, Map<Host, Boolean>> vmToStorageMotionCache = hostCache.second();

        Set<Long> vmsWithAffinityGroups = getVmsWithAffinityGroups(vmList, vmToCompatibleHostsCache);

        return getMigrationPlans(maxIterations, cluster, hostMap, vmList, vmsWithAffinityGroups, vmToCompatibleHostsCache,
                vmToStorageMotionCache, vmIdServiceOfferingMap, originalHostIdVmIdMap, hostVmMap, hostCpuMap, hostMemoryMap);
    }

    private List<Ternary<VirtualMachine, Host, Host>> getMigrationPlans(
            long maxIterations, Cluster cluster, Map<Long, Host> hostMap, List<VirtualMachine> vmList,
            Set<Long> vmsWithAffinityGroups, Map<Long, List<? extends Host>> vmToCompatibleHostsCache,
            Map<Long, Map<Host, Boolean>> vmToStorageMotionCache, Map<Long, ServiceOffering> vmIdServiceOfferingMap,
            Map<Long, List<Long>> originalHostIdVmIdMap, Map<Long, List<VirtualMachine>> hostVmMap,
            Map<Long, Ternary<Long, Long, Long>> hostCpuMap, Map<Long, Ternary<Long, Long, Long>> hostMemoryMap
    ) throws ConfigurationException {
        ClusterDrsAlgorithm algorithm = getDrsAlgorithm(ClusterDrsAlgorithm.valueIn(cluster.getId()));
        int iteration = 0;
        List<Ternary<VirtualMachine, Host, Host>> migrationPlan = new ArrayList<>();
        while (iteration < maxIterations && algorithm.needsDrs(cluster, new ArrayList<>(hostCpuMap.values()),
                new ArrayList<>(hostMemoryMap.values()))) {

            logger.debug("Starting DRS iteration {} for cluster {}", iteration + 1, cluster);
            // Re-evaluate affinity constraints with current (simulated) VM placements
            Map<Long, ExcludeList> vmToExcludesMap = getVmToExcludesMap(vmList, hostMap, vmsWithAffinityGroups,
                    vmToCompatibleHostsCache, vmIdServiceOfferingMap);

            logger.debug("Completed affinity evaluation for DRS iteration {} for cluster {}", iteration + 1, cluster);

            Pair<VirtualMachine, Host> bestMigration = getBestMigration(cluster, algorithm, vmList,
                    vmIdServiceOfferingMap, hostCpuMap, hostMemoryMap,
                    vmToCompatibleHostsCache, vmToStorageMotionCache, vmToExcludesMap);
            VirtualMachine vm = bestMigration.first();
            Host destHost = bestMigration.second();
            if (destHost == null || vm == null || originalHostIdVmIdMap.get(destHost.getId()).contains(vm.getId())) {
                logger.debug("VM migrating to it's original host or no host found for migration");
                break;
            }
            logger.debug("Plan for VM {} to migrate from host {} to host {}", vm, hostMap.get(vm.getHostId()), destHost);

            ServiceOffering serviceOffering = vmIdServiceOfferingMap.get(vm.getId());
            migrationPlan.add(new Ternary<>(vm, hostMap.get(vm.getHostId()), hostMap.get(destHost.getId())));

            hostVmMap.get(vm.getHostId()).remove(vm);
            hostVmMap.get(destHost.getId()).add(vm);

            long vmCpu = (long) serviceOffering.getCpu() * serviceOffering.getSpeed();
            long vmMemory = serviceOffering.getRamSize() * 1024L * 1024L;

            // Updating the map as per the migration
            hostCpuMap.get(vm.getHostId()).first(hostCpuMap.get(vm.getHostId()).first() - vmCpu);
            hostCpuMap.get(destHost.getId()).first(hostCpuMap.get(destHost.getId()).first() + vmCpu);
            hostMemoryMap.get(vm.getHostId()).first(hostMemoryMap.get(vm.getHostId()).first() - vmMemory);
            hostMemoryMap.get(destHost.getId()).first(hostMemoryMap.get(destHost.getId()).first() + vmMemory);
            vm.setHostId(destHost.getId());
            iteration++;
        }
        return migrationPlan;
    }

    private Map<Long, ExcludeList> getVmToExcludesMap(List<VirtualMachine> vmList, Map<Long, Host> hostMap,
            Set<Long> vmsWithAffinityGroups, Map<Long, List<? extends Host>> vmToCompatibleHostsCache,
            Map<Long, ServiceOffering> vmIdServiceOfferingMap) {
        Map<Long, ExcludeList> vmToExcludesMap = new HashMap<>();
        for (VirtualMachine vm : vmList) {
            if (vmToCompatibleHostsCache.containsKey(vm.getId())) {
                Host srcHost = hostMap.get(vm.getHostId());
                if (srcHost != null) {
                    // Only call expensive applyAffinityConstraints for VMs with affinity groups
                    // For VMs without affinity groups, create minimal ExcludeList (just source host)
                    ExcludeList excludes;
                    if (vmsWithAffinityGroups.contains(vm.getId())) {
                        DataCenterDeployment plan = new DataCenterDeployment(
                                srcHost.getDataCenterId(), srcHost.getPodId(), srcHost.getClusterId(),
                                null, null, null);
                        VirtualMachineProfile vmProfile = new VirtualMachineProfileImpl(vm, null,
                                vmIdServiceOfferingMap.get(vm.getId()), null, null);

                        excludes = managementServer.applyAffinityConstraints(
                                vm, vmProfile, plan, vmList);
                    } else {
                        // VM has no affinity groups - create minimal ExcludeList (just source host)
                        excludes = new ExcludeList();
                        excludes.addHost(vm.getHostId());
                    }
                    vmToExcludesMap.put(vm.getId(), excludes);
                }
            }
        }
        return vmToExcludesMap;
    }


    /**
     * Pre-compute suitable hosts (once per eligible VM - never changes)
     * Use listHostsForMigrationOfVM to get hosts validated by getCapableSuitableHosts
     * This ensures DRS uses the same validation as "find host for migration" command
     *
     * @param vmList List of VMs to pre-compute suitable hosts for
     * @return Pair of VM to compatible hosts map and VM to storage motion requirement map
     */
    private Pair<Map<Long, List<? extends Host>>, Map<Long, Map<Host, Boolean>>> getCompatibleHostAndVmStorageMotionCache(
            List<VirtualMachine> vmList
    ) {
        Map<Long, List<? extends Host>> vmToCompatibleHostsCache = new HashMap<>();
        Map<Long, Map<Host, Boolean>> vmToStorageMotionCache = new HashMap<>();

        List<Long> vmIds = vmList.stream().map(VirtualMachine::getId).collect(Collectors.toList());
        Set<Long> skipDrsVmIds = vmInstanceDetailsDao.listDetailsForResourceIdsAndKey(vmIds, VmDetailConstants.SKIP_DRS)
                .stream().filter(d -> "true".equalsIgnoreCase(d.getValue()))
                .map(VMInstanceDetailVO::getResourceId)
                .collect(Collectors.toSet());

        for (VirtualMachine vm : vmList) {
            // Skip ineligible VMs
            if (shouldSkipVMForDRS(vm, skipDrsVmIds)) {
                logger.debug("Skipping VM {} for DRS as it is ineligible.", vm);
                continue;
            }

            try {
                // Use listHostsForMigrationOfVM to get suitable hosts (validated by getCapableSuitableHosts)
                // This ensures the same validation as the "find host for migration" command
                Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>> hostsForMigration =
                        managementServer.listHostsForMigrationOfVM(vm, 0L, 500L, null, vmList);

                List<? extends Host> suitableHosts = hostsForMigration.second(); // Get suitable hosts (validated by HostAllocator)
                Map<Host, Boolean> requiresStorageMotion = hostsForMigration.third();

                if (suitableHosts != null && !suitableHosts.isEmpty()) {
                    vmToCompatibleHostsCache.put(vm.getId(), suitableHosts);
                    vmToStorageMotionCache.put(vm.getId(), requiresStorageMotion);
                }
            } catch (Exception e) {
                logger.debug("Could not get suitable hosts for VM {}: {}", vm, e.getMessage());
            }
        }
        return new Pair<>(vmToCompatibleHostsCache, vmToStorageMotionCache);
    }

    /**
     * Pre-fetch affinity group mappings for all eligible VMs (once, before iterations)
     * This allows us to skip expensive affinity processing for VMs without affinity groups
     *
     * @param vmList List of VMs to check for affinity groups
     * @param vmToCompatibleHostsCache Cached map of VM IDs to their compatible hosts
     * @return Set of VM IDs that have affinity groups
     */
    private Set<Long> getVmsWithAffinityGroups(
            List<VirtualMachine> vmList, Map<Long, List<? extends Host>> vmToCompatibleHostsCache
    ) {
        Set<Long> vmsWithAffinityGroups = new HashSet<>();
        for (VirtualMachine vm : vmList) {
            if (vmToCompatibleHostsCache.containsKey(vm.getId())) {
                // Check if VM has any affinity groups - if list is empty, VM has no affinity groups
                List<AffinityGroupVMMapVO> affinityGroupMappings = affinityGroupVMMapDao.listByInstanceId(vm.getId());
                if (CollectionUtils.isNotEmpty(affinityGroupMappings)) {
                    vmsWithAffinityGroups.add(vm.getId());
                }
            }
        }
        return vmsWithAffinityGroups;
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
     * @param vmToCompatibleHostsCache
     *         cached map of VM IDs to their compatible hosts
     * @param vmToStorageMotionCache
     *         cached map of VM IDs to storage motion requirements
     * @param vmToExcludesMap
     *         map of VM IDs to their ExcludeList (affinity constraints)
     *
     * @return a pair of the virtual machine and host that represent the best
     *         migration, or null if no migration is
     *         possible
     */
    Pair<VirtualMachine, Host> getBestMigration(Cluster cluster, ClusterDrsAlgorithm algorithm,
            List<VirtualMachine> vmList,
            Map<Long, ServiceOffering> vmIdServiceOfferingMap,
            Map<Long, Ternary<Long, Long, Long>> hostCpuCapacityMap,
            Map<Long, Ternary<Long, Long, Long>> hostMemoryCapacityMap,
            Map<Long, List<? extends Host>> vmToCompatibleHostsCache,
            Map<Long, Map<Host, Boolean>> vmToStorageMotionCache,
            Map<Long, ExcludeList> vmToExcludesMap) throws ConfigurationException {
        // Pre-calculate cluster imbalance once per iteration (same for all VM-host combinations)
        Double preImbalance = getClusterImbalance(cluster.getId(),
                new ArrayList<>(hostCpuCapacityMap.values()),
                new ArrayList<>(hostMemoryCapacityMap.values()),
                null);

        // Pre-calculate base metrics array once per iteration for optimized imbalance calculation
        String metricType = getClusterDrsMetric(cluster.getId());
        Map<Long, Ternary<Long, Long, Long>> baseMetricsMap = "cpu".equals(metricType) ? hostCpuCapacityMap : hostMemoryCapacityMap;
        Pair<double[], Map<Long, Integer>> baseMetricsAndIndexMap = getBaseMetricsArrayAndHostIdIndexMap(cluster, baseMetricsMap);
        double[] baseMetricsArray = baseMetricsAndIndexMap.first();
        Map<Long, Integer> hostIdToIndexMap = baseMetricsAndIndexMap.second();

        double improvement = 0;
        Pair<VirtualMachine, Host> bestMigration = new Pair<>(null, null);

        for (VirtualMachine vm : vmList) {
            List<? extends Host> compatibleHosts = vmToCompatibleHostsCache.get(vm.getId());
            Map<Host, Boolean> requiresStorageMotion = vmToStorageMotionCache.get(vm.getId());
            ExcludeList excludes = vmToExcludesMap.get(vm.getId());

            ServiceOffering serviceOffering = vmIdServiceOfferingMap.get(vm.getId());
            if (CollectionUtils.isEmpty(compatibleHosts) || serviceOffering == null) {
                continue;
            }

            long vmCpu = (long) serviceOffering.getCpu() * serviceOffering.getSpeed();
            long vmMemory = serviceOffering.getRamSize() * 1024L * 1024L;

            for (Host destHost : compatibleHosts) {
                Ternary<Double, Double, Double> metrics = getMetricsForMigration(cluster, algorithm, vm, vmCpu,
                        vmMemory, serviceOffering, destHost, hostCpuCapacityMap, hostMemoryCapacityMap,
                        requiresStorageMotion, preImbalance, baseMetricsArray, hostIdToIndexMap, excludes);
                if (metrics == null) {
                    continue;
                }
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

    private boolean shouldSkipVMForDRS(VirtualMachine vm, Set<Long> skipDrsVmIds) {
        if (vm.getType().isUsedBySystem() || vm.getState() != VirtualMachine.State.Running) {
            return true;
        }
        return skipDrsVmIds.contains(vm.getId());
    }

    private Pair<double[], Map<Long, Integer>> getBaseMetricsArrayAndHostIdIndexMap(
            Cluster cluster, Map<Long, Ternary<Long, Long, Long>> baseMetricsMap
    ) {
        double[] baseMetricsArray = new double[baseMetricsMap.size()];
        Map<Long, Integer> hostIdToIndexMap = new HashMap<>();

        int index = 0;
        for (Map.Entry<Long, Ternary<Long, Long, Long>> entry : baseMetricsMap.entrySet()) {
            Long hostId = entry.getKey();
            Ternary<Long, Long, Long> metrics = entry.getValue();
            long used = metrics.first();
            long actualTotal = metrics.third() - metrics.second();
            long free = actualTotal - metrics.first();
            Double metricValue = getMetricValue(cluster.getId(), used, free, actualTotal, null);
            if (metricValue != null) {
                baseMetricsArray[index] = metricValue;
                hostIdToIndexMap.put(hostId, index);
                index++;
            }
        }

        // Trim array if some values were null
        if (index < baseMetricsArray.length) {
            double[] trimmed = new double[index];
            System.arraycopy(baseMetricsArray, 0, trimmed, 0, index);
            baseMetricsArray = trimmed;
        }
        return new Pair<>(baseMetricsArray, hostIdToIndexMap);
    }

    private Ternary<Double, Double, Double> getMetricsForMigration(
            Cluster cluster, ClusterDrsAlgorithm algorithm, VirtualMachine vm, long vmCpu, long vmMemory,
            ServiceOffering serviceOffering, Host destHost, Map<Long, Ternary<Long, Long, Long>> hostCpuCapacityMap,
            Map<Long, Ternary<Long, Long, Long>> hostMemoryCapacityMap, Map<Host, Boolean> requiresStorageMotion,
            Double preImbalance, double[] baseMetricsArray, Map<Long, Integer> hostIdToIndexMap, ExcludeList excludes
    ) throws ConfigurationException {
        if (cluster.getId() != destHost.getClusterId()) {
            return null;
        }

        // Check affinity constraints
        if (excludes != null && excludes.shouldAvoid(destHost)) {
            return null;
        }

        // Quick capacity pre-filter: skip hosts that don't have enough capacity
        Ternary<Long, Long, Long> destHostCpu = hostCpuCapacityMap.get(destHost.getId());
        Ternary<Long, Long, Long> destHostMemory = hostMemoryCapacityMap.get(destHost.getId());
        if (destHostCpu == null || destHostMemory == null) {
            return null;
        }

        long destHostAvailableCpu = (destHostCpu.third() - destHostCpu.second()) - destHostCpu.first();
        long destHostAvailableMemory = (destHostMemory.third() - destHostMemory.second()) - destHostMemory.first();

        if (destHostAvailableCpu < vmCpu || destHostAvailableMemory < vmMemory) {
            return null; // Skip hosts without sufficient capacity
        }

        return algorithm.getMetrics(cluster, vm, serviceOffering, destHost, hostCpuCapacityMap, hostMemoryCapacityMap,
                requiresStorageMotion.getOrDefault(destHost, false), preImbalance, baseMetricsArray, hostIdToIndexMap);
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
                logger.error("Unable to execute DRS plan {}", plan, e);
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

                logger.debug("Executing DRS plan {} for vm {} to host {}", plan, vm, host);
                long jobId = createMigrateVMAsyncJob(vm, host, plan.getEventId());
                AsyncJobVO job = asyncJobManager.getAsyncJob(jobId);
                migration.setJobId(jobId);
                migration.setStatus(job.getStatus());
                drsPlanMigrationDao.update(migration.getId(), migration);
            } catch (Exception e) {
                logger.warn("Unable to execute DRS plan {} due to {}", plan, e.getMessage());
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

    protected void managePowerForAllClusters() {
        for (ClusterVO cluster : clusterDao.listAll()) {
            try {
                managePowerForCluster(cluster);
            } catch (Exception e) {
                logger.warn("DRS power management skipped cluster [{}] due to [{}].", cluster.getId(), e.getMessage(), e);
            }
        }
    }

    protected void managePowerForCluster(ClusterVO cluster) {
        if (cluster.getAllocationState() == Disabled
                || Boolean.FALSE.equals(ClusterDrsEnabled.valueIn(cluster.getId()))
                || Boolean.FALSE.equals(ClusterDrsPowerManagementEnabled.valueIn(cluster.getId()))) {
            return;
        }
        // Do not power-manage a cluster with a DRS migration plan still in flight: a host that is the source or
        // destination of a pending or running migration must not be disabled or powered off underneath it.
        if (hasInFlightDrsPlan(cluster.getId())) {
            logger.debug("DRS power management: skipping cluster [{}] while a DRS migration plan is in flight.", cluster.getId());
            return;
        }

        final float lowThreshold = ClusterDrsPowerManagementLowThreshold.valueIn(cluster.getId());
        final float highThreshold = ClusterDrsPowerManagementHighThreshold.valueIn(cluster.getId());
        final boolean useCpu = "cpu".equals(getClusterDrsMetric(cluster.getId()));

        List<HostVO> routingHosts = hostDao.findByClusterId(cluster.getId(), Host.Type.Routing);
        List<HostVO> upHosts = new ArrayList<>();
        List<HostVO> poweredOffByDrs = new ArrayList<>();
        for (HostVO host : routingHosts) {
            if (host.getStatus() == Status.Up) {
                if (isPoweredOffByDrs(host)) {
                    // Our host is back in service: either a wake completed, or a power-off we issued never took
                    // effect (command accepted but the host stayed up). Re-enable it if we had disabled it, drop
                    // the marker, and return it to the capacity pool. Keying only on Up avoids leaving such a host
                    // stranded Disabled and marked, in neither list, forever.
                    if (host.getResourceState() == ResourceState.Disabled) {
                        hostDao.updateResourceState(ResourceState.Disabled, ResourceState.Event.Enable, ResourceState.Enabled, host);
                    }
                    clearStalePowerMarker(host);
                    upHosts.add(host);
                } else if (host.getResourceState() == ResourceState.Enabled) {
                    upHosts.add(host);
                }
                // Up but Disabled by someone other than DRS: leave it alone, it is not ours to schedule onto.
            } else if (isPoweredOffByDrs(host)) {
                // Down and marked by DRS: a wake candidate. The marker is kept across the wake (power-on is
                // idempotent) and cleared only once the host is actually Up again, above.
                poweredOffByDrs.add(host);
            }
        }
        if (upHosts.isEmpty()) {
            return;
        }

        Map<Long, Ternary<Long, Long, Long>> capacityMap = getHostCapacityMap(upHosts, useCpu);
        double clusterUsed = 0d;
        double clusterTotal = 0d;
        for (Ternary<Long, Long, Long> capacity : capacityMap.values()) {
            // count used + reserved as the committed load, so a host holding HA/allocation reservations is not
            // powered off just because its live usage is low.
            clusterUsed += capacity.first() + capacity.second();
            clusterTotal += capacity.third();
        }

        // predictive DRS: fold the recent trend in. Use the more conservative of the instantaneous and forecast
        // utilization, so a rising trend wakes a host sooner and holds off power-off, while a transient dip never
        // triggers an aggressive power-off.
        double effectiveUsed = clusterUsed;
        double instantaneousRatio = clusterTotal > 0d ? clusterUsed / clusterTotal : 0d;
        if (Boolean.TRUE.equals(ClusterDrsPredictiveEnabled.valueIn(cluster.getId()))) {
            int window = ClusterDrsPredictiveWindow.valueIn(cluster.getId());
            double forecastRatio = forecastUtilization(recordAndGetUtilizationHistory(cluster.getId(), instantaneousRatio, window));
            effectiveUsed = Math.max(instantaneousRatio, forecastRatio) * clusterTotal;
        }

        // prefer restoring capacity over saving power: if the cluster is hot and we have a host we powered
        // off earlier that we can still reach over out-of-band management, bring it back before any power-off.
        if (clusterNeedsWakeup(effectiveUsed, clusterTotal, highThreshold, poweredOffByDrs.size())) {
            for (HostVO poweredOff : poweredOffByDrs) {
                if (isPowerManageable(poweredOff)) {
                    powerOnHost(poweredOff, cluster);
                    return;
                }
            }
        }

        // power off at most one empty, out-of-band-manageable host per poll, and only if the rest can carry the load.
        for (HostVO host : upHosts) {
            Ternary<Long, Long, Long> capacity = capacityMap.get(host.getId());
            if (capacity == null) {
                continue;
            }
            if (hostHasNoRunningVms(host) && isPowerManageable(host) && !isPoweredOffByDrs(host)
                    && clusterCanReleaseHost(effectiveUsed, clusterTotal, capacity.third(), upHosts.size(), lowThreshold, highThreshold)) {
                powerOffHost(host, cluster);
                return;
            }
        }

        // nothing empty to power off; if the operator opted in, drain a releasable host so a later poll can
        // power it off once empty.
        if (Boolean.TRUE.equals(ClusterDrsPowerManagementEvacuate.valueIn(cluster.getId()))) {
            evacuateReleasableHost(cluster, upHosts, capacityMap, effectiveUsed, clusterTotal, lowThreshold, highThreshold, useCpu);
        }
    }

    protected boolean isDrainingByDrs(HostVO host) {
        DetailVO detail = hostDetailsDao.findDetail(host.getId(), DRS_POWER_STATE_DETAIL);
        return detail != null && DRS_POWER_STATE_DRAINING.equals(detail.getValue());
    }

    protected boolean hasMigratingVm(long hostId) {
        List<VMInstanceVO> vms = vmInstanceDao.listByHostId(hostId);
        if (vms == null) {
            return false;
        }
        for (VMInstanceVO vm : vms) {
            if (vm.getState() == VirtualMachine.State.Migrating) {
                return true;
            }
        }
        return false;
    }

    protected void setPowerMarker(long hostId, String value) {
        DetailVO existing = hostDetailsDao.findDetail(hostId, DRS_POWER_STATE_DETAIL);
        if (existing != null) {
            hostDetailsDao.remove(existing.getId());
        }
        hostDetailsDao.persist(new DetailVO(hostId, DRS_POWER_STATE_DETAIL, value));
    }

    protected void abandonDrain(HostVO host, String reason) {
        logger.info("DRS power management: abandoning drain of host [{}]: {}", host.getId(), reason);
        clearStalePowerMarker(host);
        drainingVmCountByHost.remove(host.getId());
    }

    /**
     * Drives the eviction side of power management when no host is empty yet. Continues a drain already in progress
     * before starting another, otherwise starts draining the least-loaded releasable host. A drain that stops making
     * progress across polls (its migrations keep being rejected for affinity, host tags or storage) is abandoned
     * instead of re-submitted forever, so a host is never left indefinitely half-drained.
     */
    protected void evacuateReleasableHost(ClusterVO cluster, List<HostVO> upHosts, Map<Long, Ternary<Long, Long, Long>> capacityMap,
            double effectiveUsed, double clusterTotal, float lowThreshold, float highThreshold, boolean useCpu) {
        HostVO candidate = null;
        for (HostVO host : upHosts) {
            if (isDrainingByDrs(host)) {
                candidate = host;
                break;
            }
        }
        if (candidate == null) {
            candidate = selectDrainCandidate(upHosts, capacityMap, effectiveUsed, clusterTotal, lowThreshold, highThreshold);
        }
        if (candidate == null) {
            return;
        }
        drainHostBatch(cluster, candidate, upHosts, capacityMap, highThreshold, useCpu);
    }

    /**
     * The least-loaded host the cluster can give up: powerable, holding only running user VMs, and such that the
     * remaining hosts can still carry the load. Null if no host qualifies.
     */
    protected HostVO selectDrainCandidate(List<HostVO> upHosts, Map<Long, Ternary<Long, Long, Long>> capacityMap,
            double effectiveUsed, double clusterTotal, float lowThreshold, float highThreshold) {
        HostVO candidate = null;
        double candidateUsed = Double.MAX_VALUE;
        for (HostVO host : upHosts) {
            Ternary<Long, Long, Long> cap = capacityMap.get(host.getId());
            if (cap == null || hostHasNoRunningVms(host) || !isPowerManageable(host) || !hostEvacuatable(host)
                    || !clusterCanReleaseHost(effectiveUsed, clusterTotal, cap.third(), upHosts.size(), lowThreshold, highThreshold)) {
                continue;
            }
            if (cap.first() < candidateUsed) {
                candidate = host;
                candidateUsed = cap.first();
            }
        }
        return candidate;
    }

    /**
     * Submits one batch of migrations off {@code candidate}. Waits while a previous batch is still in flight; if the
     * host is quiescent and still holds as many VMs as at the previous batch the drain made no progress and is
     * abandoned; otherwise the host is marked draining and up to ClusterDrsMaxMigrations of its remaining VMs are
     * placed by capacity and migrated away. The host empties over successive polls and is powered off once empty.
     */
    protected void drainHostBatch(ClusterVO cluster, HostVO candidate, List<HostVO> upHosts,
            Map<Long, Ternary<Long, Long, Long>> capacityMap, float highThreshold, boolean useCpu) {
        List<VMInstanceVO> vms = vmInstanceDao.listByHostId(candidate.getId());
        if (vms == null || vms.isEmpty()) {
            // fully drained; the empty-host power-off path takes it from here.
            abandonDrain(candidate, "host is empty");
            return;
        }
        if (hasMigratingVm(candidate.getId())) {
            // a batch is still in flight; wait for it before judging progress or submitting more.
            return;
        }
        int current = vms.size();
        Integer previous = drainingVmCountByHost.get(candidate.getId());
        if (previous != null && current >= previous) {
            abandonDrain(candidate, "no progress since the last batch; remaining VMs cannot be migrated off");
            return;
        }

        Map<Long, Double> vmNeeds = new HashMap<>();
        for (VMInstanceVO vm : vms) {
            vmNeeds.put(vm.getId(), vmResourceNeed(vm, useCpu));
        }
        Map<Long, Double> hostFree = new HashMap<>();
        for (HostVO host : upHosts) {
            if (host.getId() == candidate.getId()) {
                continue;
            }
            Ternary<Long, Long, Long> cap = capacityMap.get(host.getId());
            if (cap != null) {
                // Placeable room = (high threshold of total) - (used + reserved), floored at zero, so draining
                // never pushes a destination past the high threshold it is meant to respect.
                double placeable = (double) highThreshold * cap.third() - (cap.first() + cap.second());
                hostFree.put(host.getId(), Math.max(0d, placeable));
            }
        }

        Map<Long, Long> plan = planHostEvacuation(vmNeeds, hostFree);
        if (plan.isEmpty()) {
            if (previous != null) {
                abandonDrain(candidate, "remaining VMs no longer fit on the other hosts");
            } else {
                logger.debug("DRS power management: host [{}] in cluster [{}] cannot be evacuated; not all VMs fit on the remaining hosts.",
                        candidate.getId(), cluster.getId());
            }
            return;
        }

        int maxMigrations = ClusterDrsMaxMigrations.valueIn(cluster.getId());
        logger.info("DRS power management: cluster [{}] is under-utilized; draining host [{}] ({} VMs left, up to {} per poll) to power it off.",
                cluster.getId(), candidate.getId(), plan.size(), maxMigrations);
        long eventId = ActionEventUtils.onStartedActionEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM,
                EventTypes.EVENT_VM_MIGRATE,
                String.format("DRS power management draining host %d in cluster %s", candidate.getId(), cluster.getUuid()),
                candidate.getId(), ApiCommandResourceType.Host.toString(), true, 0);
        int submitted = 0;
        for (Map.Entry<Long, Long> entry : plan.entrySet()) {
            if (submitted >= maxMigrations) {
                break;
            }
            VirtualMachine vm = vmInstanceDao.findById(entry.getKey());
            HostVO destination = hostDao.findById(entry.getValue());
            if (vm != null && destination != null) {
                createMigrateVMAsyncJob(vm, destination, eventId);
                submitted++;
            }
        }
        if (submitted > 0) {
            setPowerMarker(candidate.getId(), DRS_POWER_STATE_DRAINING);
            drainingVmCountByHost.put(candidate.getId(), current);
        }
    }

    protected double vmResourceNeed(VMInstanceVO vm, boolean useCpu) {
        ServiceOffering offering = serviceOfferingDao.findByIdIncludingRemoved(vm.getId(), vm.getServiceOfferingId());
        if (offering == null) {
            return 0d;
        }
        if (useCpu) {
            return (double) offering.getCpu() * offering.getSpeed();
        }
        return (double) offering.getRamSize() * 1024L * 1024L;
    }

    /**
     * First-fit-decreasing placement of the given VMs (id to resource need) onto hosts (id to free capacity),
     * returning a VM-to-host assignment or an empty map if any VM does not fit on capacity alone. This is a
     * capacity feasibility pre-check only: it does NOT account for affinity/anti-affinity, host tags, storage
     * access or dedication. Each migration is still validated authoritatively by the migration job, which may
     * reject a placement this map proposed; in that case the host is drained over later polls rather than at once.
     * Pure function, unit-tested.
     */
    protected Map<Long, Long> planHostEvacuation(Map<Long, Double> vmNeeds, Map<Long, Double> hostFreeCapacity) {
        Map<Long, Long> assignment = new HashMap<>();
        // TreeMap so host iteration is by id and placement is deterministic run-to-run; secondary sort by vm id
        // breaks ties among equal-sized VMs for the same reason.
        Map<Long, Double> remaining = new TreeMap<>(hostFreeCapacity);
        List<Map.Entry<Long, Double>> vms = new ArrayList<>(vmNeeds.entrySet());
        vms.sort((a, b) -> b.getValue().equals(a.getValue()) ? Long.compare(a.getKey(), b.getKey()) : Double.compare(b.getValue(), a.getValue()));
        for (Map.Entry<Long, Double> vm : vms) {
            Long target = null;
            for (Map.Entry<Long, Double> host : remaining.entrySet()) {
                if (host.getValue() >= vm.getValue()) {
                    target = host.getKey();
                    break;
                }
            }
            if (target == null) {
                return Collections.emptyMap();
            }
            assignment.put(vm.getKey(), target);
            remaining.put(target, remaining.get(target) - vm.getValue());
        }
        return assignment;
    }

    /**
     * Appends the latest cluster utilization ratio to the cluster's rolling history (trimmed to {@code window}
     * samples) and returns a snapshot of it, newest last.
     */
    protected List<Double> recordAndGetUtilizationHistory(long clusterId, double ratio, int window) {
        int cap = Math.max(1, window);
        LinkedList<Double> history = clusterUtilizationHistory.computeIfAbsent(clusterId, k -> new LinkedList<>());
        synchronized (history) {
            history.addLast(ratio);
            while (history.size() > cap) {
                history.removeFirst();
            }
            return new ArrayList<>(history);
        }
    }

    /**
     * Projects one poll interval ahead from the recent utilization samples using the least-squares trend, clamped
     * to [0, 1]. With fewer than two samples it returns the latest sample (no trend to project).
     */
    protected double forecastUtilization(List<Double> history) {
        if (history == null || history.isEmpty()) {
            return 0d;
        }
        int n = history.size();
        if (n == 1) {
            return history.get(0);
        }
        double sumX = 0d;
        double sumY = 0d;
        double sumXY = 0d;
        double sumXX = 0d;
        for (int i = 0; i < n; i++) {
            double y = history.get(i);
            sumX += i;
            sumY += y;
            sumXY += i * y;
            sumXX += (double) i * i;
        }
        double denominator = n * sumXX - sumX * sumX;
        double slope = denominator == 0d ? 0d : (n * sumXY - sumX * sumY) / denominator;
        double forecast = history.get(n - 1) + slope;
        return Math.max(0d, Math.min(1d, forecast));
    }

    protected Map<Long, Ternary<Long, Long, Long>> getHostCapacityMap(List<HostVO> hosts, boolean useCpu) {
        List<HostJoinVO> joins = hostJoinDao.searchByIds(hosts.stream().map(HostVO::getId).toArray(Long[]::new));
        Map<Long, Ternary<Long, Long, Long>> map = new HashMap<>();
        for (HostJoinVO join : joins) {
            if (useCpu) {
                Integer cpus = join.getCpus();
                Long speed = join.getSpeed();
                long cpuTotal = (cpus == null || speed == null) ? 0L : (long) cpus * speed;
                map.put(join.getId(), new Ternary<>(join.getCpuUsedCapacity(), join.getCpuReservedCapacity(), cpuTotal));
            } else {
                map.put(join.getId(), new Ternary<>(join.getMemUsedCapacity(), join.getMemReservedCapacity(), join.getTotalMemory()));
            }
        }
        return map;
    }

    /**
     * True when the cluster can give up the candidate host: the cluster is below the low utilization threshold,
     * there is more than one host up, and the remaining hosts can carry the current load without crossing the
     * high threshold.
     */
    protected boolean clusterCanReleaseHost(double clusterUsed, double clusterTotal, double candidateHostTotal,
            int upHostCount, float lowThreshold, float highThreshold) {
        if (upHostCount <= 1 || clusterTotal <= 0d) {
            return false;
        }
        if ((clusterUsed / clusterTotal) >= lowThreshold) {
            return false;
        }
        double remainingTotal = clusterTotal - candidateHostTotal;
        if (remainingTotal <= 0d) {
            return false;
        }
        return (clusterUsed / remainingTotal) <= highThreshold;
    }

    /**
     * True when the cluster is above the high utilization threshold and there is a host that DRS powered off
     * earlier which can be powered back on.
     */
    protected boolean clusterNeedsWakeup(double clusterUsed, double clusterTotal, float highThreshold, int poweredOffHostCount) {
        if (poweredOffHostCount <= 0 || clusterTotal <= 0d) {
            return false;
        }
        return (clusterUsed / clusterTotal) > highThreshold;
    }

    protected boolean hostHasNoRunningVms(HostVO host) {
        List<VMInstanceVO> vms = vmInstanceDao.listByHostId(host.getId());
        return vms == null || vms.isEmpty();
    }

    protected boolean isPowerManageable(HostVO host) {
        try {
            return outOfBandManagementService.isOutOfBandManagementEnabled(host);
        } catch (Exception e) {
            logger.debug("Could not determine out-of-band management status for host [{}]: {}", host.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * A host can be drained by power management only if every VM on it is a running user VM: a system VM cannot be
     * moved with a user-VM migration, and a VM in a transitional state must not be forced, so such a host is never
     * chosen (it could not be fully emptied).
     */
    protected boolean hostEvacuatable(HostVO host) {
        List<VMInstanceVO> vms = vmInstanceDao.listByHostId(host.getId());
        if (vms == null || vms.isEmpty()) {
            return false;
        }
        for (VMInstanceVO vm : vms) {
            if (vm.getType().isUsedBySystem() || vm.getState() != VirtualMachine.State.Running) {
                return false;
            }
        }
        return true;
    }

    protected boolean isPoweredOffByDrs(HostVO host) {
        DetailVO detail = hostDetailsDao.findDetail(host.getId(), DRS_POWER_STATE_DETAIL);
        return detail != null && DRS_POWER_STATE_OFF.equals(detail.getValue());
    }

    protected void clearStalePowerMarker(HostVO host) {
        DetailVO detail = hostDetailsDao.findDetail(host.getId(), DRS_POWER_STATE_DETAIL);
        if (detail != null) {
            hostDetailsDao.remove(detail.getId());
        }
    }

    protected boolean hasInFlightDrsPlan(long clusterId) {
        return !drsPlanDao.listByClusterIdAndStatus(clusterId, ClusterDrsPlan.Status.UNDER_REVIEW).isEmpty()
                || !drsPlanDao.listByClusterIdAndStatus(clusterId, ClusterDrsPlan.Status.READY).isEmpty()
                || !drsPlanDao.listByClusterIdAndStatus(clusterId, ClusterDrsPlan.Status.IN_PROGRESS).isEmpty();
    }

    protected void powerOffHost(HostVO host, ClusterVO cluster) {
        logger.info("DRS power management: cluster [{}] is under-utilized; disabling and powering off empty host [{}].", cluster.getId(), host.getId());
        // Disable first so CloudStack stops scheduling to the host and does not treat the imminent agent
        // disconnect as a failure (host monitor / HA). Abort if the transition did not take effect, so the
        // host is never powered off while CloudStack still believes it is schedulable.
        if (!hostDao.updateResourceState(ResourceState.Enabled, ResourceState.Event.Disable, ResourceState.Disabled, host)) {
            logger.warn("DRS power management: could not disable host [{}]; skipping power-off.", host.getId());
            return;
        }
        // Record the durable wake intent BEFORE the irreversible power-off. If the management server dies between
        // the power-off and here, the host is still recognised on the next poll and powered back on, rather than
        // left off forever with no marker. Replace any existing marker (e.g. a draining marker when powering off a
        // host that has just finished draining) so only the off marker remains.
        DetailVO existing = hostDetailsDao.findDetail(host.getId(), DRS_POWER_STATE_DETAIL);
        if (existing != null) {
            hostDetailsDao.remove(existing.getId());
        }
        DetailVO marker = new DetailVO(host.getId(), DRS_POWER_STATE_DETAIL, DRS_POWER_STATE_OFF);
        hostDetailsDao.persist(marker);
        try {
            outOfBandManagementService.executePowerOperation(host, OutOfBandManagement.PowerOperation.OFF, null);
        } catch (Exception e) {
            // Power-off failed: undo the marker and the disable so the host stays in service.
            hostDetailsDao.remove(marker.getId());
            hostDao.updateResourceState(ResourceState.Disabled, ResourceState.Event.Enable, ResourceState.Enabled, host);
            throw e;
        }
        drainingVmCountByHost.remove(host.getId());
    }

    protected void powerOnHost(HostVO host, ClusterVO cluster) {
        logger.info("DRS power management: cluster [{}] is over-utilized; powering on host [{}].", cluster.getId(), host.getId());
        // Issue the power-on but keep the marker and leave the host Disabled: the host is still down until its
        // agent reconnects, and an accepted-but-unconfirmed power-on must not look done. The classification loop
        // re-enables the host and clears the marker only once it is actually Up. Power-on is idempotent, so a host
        // still booting is simply re-issued the command on a later poll until it connects.
        outOfBandManagementService.executePowerOperation(host, OutOfBandManagement.PowerOperation.ON, null);
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{ClusterDrsPlanExpireInterval, ClusterDrsEnabled, ClusterDrsInterval, ClusterDrsMaxMigrations,
                ClusterDrsAlgorithm, ClusterDrsImbalanceThreshold, ClusterDrsMetric, ClusterDrsMetricType, ClusterDrsMetricUseRatio,
                ClusterDrsImbalanceSkipThreshold, ClusterDrsPowerManagementEnabled, ClusterDrsPowerManagementLowThreshold,
                ClusterDrsPowerManagementHighThreshold, ClusterDrsPredictiveEnabled, ClusterDrsPredictiveWindow,
                ClusterDrsPowerManagementEvacuate};
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
