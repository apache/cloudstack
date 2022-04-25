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
package com.cloud.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.affinity.AffinityGroupProcessor;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.command.admin.cluster.UpdateClusterCmd;
import org.apache.cloudstack.api.command.admin.host.PrepareForMaintenanceCmd;
import org.apache.cloudstack.api.command.admin.resource.StartRollingMaintenanceCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.RollingMaintenanceAnswer;
import com.cloud.agent.api.RollingMaintenanceCommand;
import com.cloud.alert.AlertManager;
import com.cloud.capacity.CapacityManager;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.deploy.DeployDestination;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventVO;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostTagsDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.org.Cluster;
import com.cloud.org.Grouping;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineProfileImpl;
import com.cloud.vm.dao.VMInstanceDao;

public class RollingMaintenanceManagerImpl extends ManagerBase implements RollingMaintenanceManager {

    @Inject
    private HostDao hostDao;
    @Inject
    private AgentManager agentManager;
    @Inject
    private ResourceManager resourceManager;
    @Inject
    private CapacityManager capacityManager;
    @Inject
    private VMInstanceDao vmInstanceDao;
    @Inject
    private ServiceOfferingDao serviceOfferingDao;
    @Inject
    private ClusterDetailsDao clusterDetailsDao;
    @Inject
    private HostTagsDao hostTagsDao;
    @Inject
    private AlertManager alertManager;

    protected List<AffinityGroupProcessor> _affinityProcessors;

    public void setAffinityGroupProcessors(List<AffinityGroupProcessor> affinityProcessors) {
        _affinityProcessors = affinityProcessors;
    }

    public static final Logger s_logger = Logger.getLogger(RollingMaintenanceManagerImpl.class.getName());

    private Pair<ResourceType, List<Long>> getResourceTypeAndIdPair(List<Long> podIds, List<Long> clusterIds, List<Long> zoneIds, List<Long> hostIds) {
        Pair<ResourceType, List<Long>> pair = CollectionUtils.isNotEmpty(podIds) ? new Pair<>(ResourceType.Pod, podIds) :
               CollectionUtils.isNotEmpty(clusterIds) ? new Pair<>(ResourceType.Cluster, clusterIds) :
               CollectionUtils.isNotEmpty(zoneIds) ? new Pair<>(ResourceType.Zone, zoneIds) :
               CollectionUtils.isNotEmpty(hostIds) ? new Pair<>(ResourceType.Host, hostIds) : null;
        if (pair == null) {
            throw new CloudRuntimeException("Parameters podId, clusterId, zoneId, hostId are mutually exclusive, " +
                    "please set only one of them");
        }
        return pair;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        return true;
    }

    private void updateCluster(long clusterId, String allocationState) {
        Cluster cluster = resourceManager.getCluster(clusterId);
        if (cluster == null) {
            throw new InvalidParameterValueException("Unable to find the cluster by id=" + clusterId);
        }
        UpdateClusterCmd updateClusterCmd = new UpdateClusterCmd();
        updateClusterCmd.setId(clusterId);
        updateClusterCmd.setAllocationState(allocationState);
        resourceManager.updateCluster(updateClusterCmd);
    }

    private void generateReportAndFinishingEvent(StartRollingMaintenanceCmd cmd, boolean success, String details,
                                                 List<HostUpdated> hostsUpdated, List<HostSkipped> hostsSkipped) {
        Pair<ResourceType, List<Long>> pair = getResourceTypeIdPair(cmd);
        ResourceType entity = pair.first();
        List<Long> ids = pair.second();
        String cmdResourceType = ApiCommandResourceType.fromString(entity.name()) != null ? ApiCommandResourceType.fromString(entity.name()).toString() : null;
        String description = String.format("Success: %s, details: %s, hosts updated: %s, hosts skipped: %s", success, details,
                generateReportHostsUpdated(hostsUpdated), generateReportHostsSkipped(hostsSkipped));
        ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(), CallContext.current().getCallingAccountId(),
                EventVO.LEVEL_INFO, cmd.getEventType(),
                "Completed rolling maintenance for entity " + entity + " with IDs: " + ids + " - " + description, ids.get(0), cmdResourceType, 0);
    }

    private String generateReportHostsUpdated(List<HostUpdated> hostsUpdated) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(hostsUpdated.size());
        return stringBuilder.toString();
    }

    private String generateReportHostsSkipped(List<HostSkipped> hostsSkipped) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(hostsSkipped.size());
        return stringBuilder.toString();
    }

    @Override
    public Ternary<Boolean, String, Pair<List<HostUpdated>, List<HostSkipped>>> startRollingMaintenance(StartRollingMaintenanceCmd cmd) {
        Pair<ResourceType, List<Long>> pair = getResourceTypeAndIdPair(cmd.getPodIds(), cmd.getClusterIds(), cmd.getZoneIds(), cmd.getHostIds());
        ResourceType type = pair.first();
        List<Long> ids = pair.second();
        int timeout = cmd.getTimeout() == null ? KvmRollingMaintenanceStageTimeout.value() : cmd.getTimeout();
        String payload = cmd.getPayload();
        Boolean forced = cmd.getForced();

        Set<Long> disabledClusters = new HashSet<>();
        Map<Long, String> hostsToAvoidMaintenance = new HashMap<>();

        boolean success = false;
        String details = null;
        List<HostUpdated> hostsUpdated = new ArrayList<>();
        List<HostSkipped> hostsSkipped = new ArrayList<>();

        if (timeout <= KvmRollingMaintenancePingInterval.value()) {
            return new Ternary<>(success, "The timeout value provided must be greater or equal than the ping interval " +
                    "defined in '" + KvmRollingMaintenancePingInterval.key() + "'", new Pair<>(hostsUpdated, hostsSkipped));
        }

        try {
            Map<Long, List<Host>> hostsByCluster = getHostsByClusterForRollingMaintenance(type, ids);

            for (Long clusterId : hostsByCluster.keySet()) {
                Cluster cluster = resourceManager.getCluster(clusterId);
                List<Host> hosts = hostsByCluster.get(clusterId);

                if (!isMaintenanceAllowedByVMStates(cluster, hosts, hostsSkipped)) {
                    if (forced) {
                        continue;
                    }
                    success = false;
                    details = "VMs in invalid states in cluster: " + cluster.getUuid();
                    return new Ternary<>(success, details, new Pair<>(hostsUpdated, hostsSkipped));
                }
                disableClusterIfEnabled(cluster, disabledClusters);

                s_logger.debug("State checks on the hosts in the cluster");
                performStateChecks(cluster, hosts, forced, hostsSkipped);
                s_logger.debug("Checking hosts capacity before attempting rolling maintenance");
                performCapacityChecks(cluster, hosts, forced);
                s_logger.debug("Attempting pre-flight stages on each host before starting rolling maintenance");
                performPreFlightChecks(hosts, timeout, payload, forced, hostsToAvoidMaintenance);

                for (Host host: hosts) {
                    Ternary<Boolean, Boolean, String> hostResult = startRollingMaintenanceHostInCluster(cluster, host,
                            timeout, payload, forced, hostsToAvoidMaintenance, hostsUpdated, hostsSkipped);
                    if (hostResult.second()) {
                        continue;
                    }
                    if (hostResult.first()) {
                        success = false;
                        details = hostResult.third();
                        return new Ternary<>(success, details, new Pair<>(hostsUpdated, hostsSkipped));
                    }
                }
                enableClusterIfDisabled(cluster, disabledClusters);
            }
        } catch (AgentUnavailableException | InterruptedException | CloudRuntimeException e) {
            String err = "Error starting rolling maintenance: " + e.getMessage();
            s_logger.error(err, e);
            success = false;
            details = err;
            return new Ternary<>(success, details, new Pair<>(hostsUpdated, hostsSkipped));
        } finally {
            // Enable back disabled clusters
            for (Long clusterId : disabledClusters) {
                Cluster cluster = resourceManager.getCluster(clusterId);
                if (cluster.getAllocationState() == Grouping.AllocationState.Disabled) {
                    updateCluster(clusterId, "Enabled");
                }
            }
            generateReportAndFinishingEvent(cmd, success, details, hostsUpdated, hostsSkipped);
        }
        success = true;
        details = "OK";
        return new Ternary<>(success, details, new Pair<>(hostsUpdated, hostsSkipped));
    }

    /**
     * Perform state checks on the hosts in a cluster
     */
    protected void performStateChecks(Cluster cluster, List<Host> hosts, Boolean forced, List<HostSkipped> hostsSkipped) {
        List<Host> hostsToDrop = new ArrayList<>();
        for (Host host : hosts) {
            if (host.getStatus() != Status.Up) {
                String msg = "Host " + host.getUuid() + " is not connected, state = " + host.getStatus().toString();
                if (forced) {
                    hostsSkipped.add(new HostSkipped(host, msg));
                    hostsToDrop.add(host);
                    continue;
                }
                throw new CloudRuntimeException(msg);
            }
            if (host.getResourceState() != ResourceState.Enabled) {
                String msg = "Host " + host.getUuid() + " is not enabled, state = " + host.getResourceState().toString();
                if (forced) {
                    hostsSkipped.add(new HostSkipped(host, msg));
                    hostsToDrop.add(host);
                    continue;
                }
                throw new CloudRuntimeException(msg);
            }
        }
        if (CollectionUtils.isNotEmpty(hostsToDrop)) {
            hosts.removeAll(hostsToDrop);
        }
    }

    /**
     * Do not allow rolling maintenance if there are VMs in Starting/Stopping/Migrating/Error/Unknown state
     */
    private boolean isMaintenanceAllowedByVMStates(Cluster cluster, List<Host> hosts, List<HostSkipped> hostsSkipped) {
        for (Host host : hosts) {
            List<VMInstanceVO> notAllowedStates = vmInstanceDao.findByHostInStates(host.getId(), State.Starting, State.Stopping,
                    State.Migrating, State.Error, State.Unknown);
            if (notAllowedStates.size() > 0) {
                String msg = "There are VMs in starting/stopping/migrating/error/unknown state, not allowing rolling maintenance in the cluster";
                HostSkipped skipped = new HostSkipped(host, msg);
                hostsSkipped.add(skipped);
                return false;
            }
        }
        return true;
    }

    /**
     * Start rolling maintenance for a single host
     * @return tuple: (FAIL, SKIP, DETAILS), where:
     *                  - FAIL: True if rolling maintenance must fail
     *                  - SKIP: True if host must be skipped
     *                  - DETAILS: Information retrieved by the host
     */
    private Ternary<Boolean, Boolean, String> startRollingMaintenanceHostInCluster(Cluster cluster, Host host, int timeout,
                                                                                   String payload, Boolean forced,
                                                                                   Map<Long, String> hostsToAvoidMaintenance,
                                                                                   List<HostUpdated> hostsUpdated,
                                                                                   List<HostSkipped> hostsSkipped) throws InterruptedException, AgentUnavailableException {
        Ternary<Boolean, Boolean, String> result;
        if (!isMaintenanceScriptDefinedOnHost(host, hostsSkipped)) {
            String msg = "There is no maintenance script on the host";
            hostsSkipped.add(new HostSkipped(host, msg));
            return new Ternary<>(false, true, msg);
        }

        result = performPreMaintenanceStageOnHost(host, timeout, payload, forced, hostsToAvoidMaintenance, hostsSkipped);
        if (result.first() || result.second()) {
            return result;
        }

        if (isMaintenanceStageAvoided(host, hostsToAvoidMaintenance, hostsSkipped)) {
            return new Ternary<>(false, true, "Maintenance stage must be avoided");
        }

        s_logger.debug("Updating capacity before re-checking capacity");
        alertManager.recalculateCapacity();
        result = reCheckCapacityBeforeMaintenanceOnHost(cluster, host, forced, hostsSkipped);
        if (result.first() || result.second()) {
            return result;
        }

        Date startTime = new Date();
        putHostIntoMaintenance(host);
        result = performMaintenanceStageOnHost(host, timeout, payload, forced, hostsToAvoidMaintenance, hostsSkipped);
        if (result.first() || result.second()) {
            cancelHostMaintenance(host);
            return result;
        }
        cancelHostMaintenance(host);
        Date endTime = new Date();

        HostUpdated hostUpdated = new HostUpdated(host, startTime, endTime, result.third());
        hostsUpdated.add(hostUpdated);

        result = performPostMaintenanceStageOnHost(host, timeout, payload, forced, hostsToAvoidMaintenance, hostsSkipped);
        if (result.first() || result.second()) {
            return result;
        }
        return new Ternary<>(false, false, "Completed rolling maintenance on host " + host.getUuid());
    }

    /**
     * Perform Post-Maintenance stage on host
     * @return tuple: (FAIL, SKIP, DETAILS), where:
     *                  - FAIL: True if rolling maintenance must fail
     *                  - SKIP: True if host must be skipped
     *                  - DETAILS: Information retrieved by the host after executing the stage
     * @throws InterruptedException
     */
    private Ternary<Boolean, Boolean, String> performPostMaintenanceStageOnHost(Host host, int timeout, String payload, Boolean forced, Map<Long, String> hostsToAvoidMaintenance, List<HostSkipped> hostsSkipped) throws InterruptedException {
        Ternary<Boolean, String, Boolean> result = performStageOnHost(host, Stage.PostMaintenance, timeout, payload, forced);
        if (!result.first()) {
            if (forced) {
                String msg = "Post-maintenance script failed: " + result.second();
                hostsSkipped.add(new HostSkipped(host, msg));
                return new Ternary<>(true, true, msg);
            }
            return new Ternary<>(true, false, result.second());
        }
        return new Ternary<>(false, false, result.second());
    }

    /**
     * Cancel maintenance mode on host
     * @param host host
     */
    private void cancelHostMaintenance(Host host) {
        if (!resourceManager.cancelMaintenance(host.getId())) {
            String message = "Could not cancel maintenance on host " + host.getUuid();
            s_logger.error(message);
            throw new CloudRuntimeException(message);
        }
    }

    /**
     * Perform Maintenance stage on host
     * @return tuple: (FAIL, SKIP, DETAILS), where:
     *                  - FAIL: True if rolling maintenance must fail
     *                  - SKIP: True if host must be skipped
     *                  - DETAILS: Information retrieved by the host after executing the stage
     * @throws InterruptedException
     */
    private Ternary<Boolean, Boolean, String> performMaintenanceStageOnHost(Host host, int timeout, String payload, Boolean forced, Map<Long, String> hostsToAvoidMaintenance, List<HostSkipped> hostsSkipped) throws InterruptedException {
        Ternary<Boolean, String, Boolean> result = performStageOnHost(host, Stage.Maintenance, timeout, payload, forced);
        if (!result.first()) {
            if (forced) {
                String msg = "Maintenance script failed: " + result.second();
                hostsSkipped.add(new HostSkipped(host, msg));
                return new Ternary<>(true, true, msg);
            }
            return new Ternary<>(true, false, result.second());
        }
        return new Ternary<>(false, false, result.second());
    }

    /**
     * Puts host into maintenance and waits for its completion
     * @param host host
     * @throws InterruptedException
     * @throws AgentUnavailableException
     */
    private void putHostIntoMaintenance(Host host) throws InterruptedException, AgentUnavailableException {
        s_logger.debug(String.format("Trying to set %s into maintenance", host));
        PrepareForMaintenanceCmd cmd = new PrepareForMaintenanceCmd();
        cmd.setId(host.getId());
        resourceManager.maintain(cmd);
        waitForHostInMaintenance(host.getId());
    }

    /**
     * Enable back disabled cluster
     * @param cluster cluster to enable if it has been disabled
     * @param disabledClusters set of disabled clusters
     */
    private void enableClusterIfDisabled(Cluster cluster, Set<Long> disabledClusters) {
        if (cluster.getAllocationState() == Grouping.AllocationState.Disabled && disabledClusters.contains(cluster.getId())) {
            updateCluster(cluster.getId(), "Enabled");
        }
    }

    /**
     * Re-check capacity to ensure the host can transit into maintenance state
     * @return tuple: (FAIL, SKIP, DETAILS), where:
     *                  - FAIL: True if rolling maintenance must fail
     *                  - SKIP: True if host must be skipped
     *                  - DETAILS: Information retrieved after capacity checks
     */
    private Ternary<Boolean, Boolean, String> reCheckCapacityBeforeMaintenanceOnHost(Cluster cluster, Host host, Boolean forced, List<HostSkipped> hostsSkipped) {
        Pair<Boolean, String> capacityCheckBeforeMaintenance = performCapacityChecksBeforeHostInMaintenance(host, cluster);
        if (!capacityCheckBeforeMaintenance.first()) {
            String errorMsg = String.format("Capacity check failed for %s: %s", host, capacityCheckBeforeMaintenance.second());
            if (forced) {
                s_logger.info(String.format("Skipping %s as: %s", host, errorMsg));
                hostsSkipped.add(new HostSkipped(host, errorMsg));
                return new Ternary<>(true, true, capacityCheckBeforeMaintenance.second());
            }
            return new Ternary<>(true, false, capacityCheckBeforeMaintenance.second());
        }
        return new Ternary<>(false, false, capacityCheckBeforeMaintenance.second());
    }

    /**
     * Indicates if the maintenance stage must be avoided
     */
    private boolean isMaintenanceStageAvoided(Host host, Map<Long, String> hostsToAvoidMaintenance, List<HostSkipped> hostsSkipped) {
        if (hostsToAvoidMaintenance.containsKey(host.getId())) {
            HostSkipped hostSkipped = new HostSkipped(host, hostsToAvoidMaintenance.get(host.getId()));
            hostsSkipped.add(hostSkipped);
            s_logger.debug(String.format("%s is in avoid maintenance list [hosts skipped: %d], skipping its maintenance.", host, hostsSkipped.size()));
            return true;
        }
        return false;
    }

    /**
     * Perform Pre-Maintenance stage on host
     * @return tuple: (FAIL, SKIP, DETAILS), where:
     *                  - FAIL: True if rolling maintenance must fail
     *                  - SKIP: True if host must be skipped
     *                  - DETAILS: Information retrieved by the host after executing the stage
     * @throws InterruptedException
     */
    private Ternary<Boolean, Boolean, String> performPreMaintenanceStageOnHost(Host host, int timeout, String payload, Boolean forced,
                                                                               Map<Long, String> hostsToAvoidMaintenance,
                                                                               List<HostSkipped> hostsSkipped) throws InterruptedException {
        Ternary<Boolean, String, Boolean> result = performStageOnHost(host, Stage.PreMaintenance, timeout, payload, forced);
        if (!result.first()) {
            if (forced) {
                String msg = "Pre-maintenance script failed: " + result.second();
                hostsSkipped.add(new HostSkipped(host, msg));
                return new Ternary<>(true, true, result.second());
            }
            return new Ternary<>(true, false, result.second());
        }
        if (result.third() && !hostsToAvoidMaintenance.containsKey(host.getId())) {
            logHostAddedToAvoidMaintenanceSet(host);
            hostsToAvoidMaintenance.put(host.getId(), "Pre-maintenance stage set to avoid maintenance");
        }
        return new Ternary<>(false, false, result.second());
    }

    /**
     * Disable cluster (if hasn't been disabled yet)
     * @param cluster cluster to disable
     * @param disabledClusters set of disabled cluster ids. cluster is added if it is disabled
     */
    private void disableClusterIfEnabled(Cluster cluster, Set<Long> disabledClusters) {
        if (cluster.getAllocationState() == Grouping.AllocationState.Enabled && !disabledClusters.contains(cluster.getId())) {
            updateCluster(cluster.getId(), "Disabled");
            disabledClusters.add(cluster.getId());
        }
    }

    private boolean isMaintenanceScriptDefinedOnHost(Host host, List<HostSkipped> hostsSkipped) {
        try {
            RollingMaintenanceAnswer answer = (RollingMaintenanceAnswer) agentManager.send(host.getId(), new RollingMaintenanceCommand(true));
            return answer.isMaintenaceScriptDefined();
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            String msg = String.format("Could not check for maintenance script on %s due to: %s", host, e.getMessage());
            s_logger.error(msg, e);
            return false;
        }
    }

    /**
     * Execute stage on host
     * @return tuple: (SUCCESS, DETAILS, AVOID_MAINTENANCE) where:
     *                  - SUCCESS: True if stage is successful
     *                  - DETAILS: Information retrieved by the host after executing the stage
     *                  - AVOID_MAINTENANCE: True if maintenance stage must be avoided
     */
    private Ternary<Boolean, String, Boolean> performStageOnHost(Host host, Stage stage, int timeout,
                                                                String payload, Boolean forced) throws InterruptedException {
        Ternary<Boolean, String, Boolean> result = sendRollingMaintenanceCommandToHost(host, stage, timeout, payload);
        if (!result.first() && !forced) {
            throw new CloudRuntimeException("Stage: " + stage.toString() + " failed on host " + host.getUuid() + ": " + result.second());
        }
        return result;
    }

    /**
     * Send rolling maintenance command to a host to perform a certain stage specified in cmd
     * @return tuple: (SUCCESS, DETAILS, AVOID_MAINTENANCE) where:
     *                  - SUCCESS: True if stage is successful
     *                  - DETAILS: Information retrieved by the host after executing the stage
     *                  - AVOID_MAINTENANCE: True if maintenance stage must be avoided
     */
    private Ternary<Boolean, String, Boolean> sendRollingMaintenanceCommandToHost(Host host, Stage stage,
                                                                                 int timeout, String payload) throws InterruptedException {
        boolean completed = false;
        Answer answer = null;
        long timeSpent = 0L;
        long pingInterval = KvmRollingMaintenancePingInterval.value() * 1000L;
        boolean avoidMaintenance = false;

        RollingMaintenanceCommand cmd = new RollingMaintenanceCommand(stage.toString());
        cmd.setWait(timeout);
        cmd.setPayload(payload);

        while (!completed && timeSpent < timeout * 1000L) {
            try {
                answer = agentManager.send(host.getId(), cmd);
            } catch (AgentUnavailableException | OperationTimedoutException e) {
                // Agent may be restarted on the scripts - continue polling until it is up
                String msg = String.format("Cannot send command to %s, waiting %sms - %s", host, pingInterval, e.getMessage());
                s_logger.warn(msg, e);
                cmd.setStarted(true);
                Thread.sleep(pingInterval);
                timeSpent += pingInterval;
                continue;
            }
            cmd.setStarted(true);

            RollingMaintenanceAnswer rollingMaintenanceAnswer = (RollingMaintenanceAnswer) answer;
            completed = rollingMaintenanceAnswer.isFinished();
            if (!completed) {
                Thread.sleep(pingInterval);
                timeSpent += pingInterval;
            } else {
                avoidMaintenance = rollingMaintenanceAnswer.isAvoidMaintenance();
            }
        }
        if (timeSpent >= timeout * 1000L) {
            return new Ternary<>(false,
                    "Timeout exceeded for rolling maintenance on host " + host.getUuid() + " and stage " + stage.toString(),
                    avoidMaintenance);
        }
        return new Ternary<>(answer.getResult(), answer.getDetails(), avoidMaintenance);
    }

    /**
     * Pre flight checks on hosts
     */
    private void performPreFlightChecks(List<Host> hosts, int timeout, String payload, Boolean forced,
                                        Map<Long, String> hostsToAvoidMaintenance) throws InterruptedException {
        for (Host host : hosts) {
            Ternary<Boolean, String, Boolean> result = performStageOnHost(host, Stage.PreFlight, timeout, payload, forced);
            if (result.third() && !hostsToAvoidMaintenance.containsKey(host.getId())) {
                logHostAddedToAvoidMaintenanceSet(host);
                hostsToAvoidMaintenance.put(host.getId(), "Pre-flight stage set to avoid maintenance");
            }
        }
    }

    private void logHostAddedToAvoidMaintenanceSet(Host host) {
        s_logger.debug(String.format("%s added to the avoid maintenance set.", host));
    }

    /**
     * Capacity checks on hosts
     */
    private void performCapacityChecks(Cluster cluster, List<Host> hosts, Boolean forced) {
        for (Host host : hosts) {
            Pair<Boolean, String> result = performCapacityChecksBeforeHostInMaintenance(host, cluster);
            if (!result.first() && !forced) {
                throw new CloudRuntimeException(String.format("Capacity check failed for %s : %s", host, result.second()));
            }
        }
    }

    /**
     * Check if there is enough capacity for host to enter maintenance
     */
    private Pair<Boolean, String> performCapacityChecksBeforeHostInMaintenance(Host host, Cluster cluster) {
        List<HostVO> hosts = hostDao.findByClusterId(cluster.getId());
        List<Host> hostsInCluster = hosts.stream()
                .filter(x -> x.getId() != host.getId() &&
                        x.getClusterId().equals(cluster.getId()) &&
                        x.getResourceState() == ResourceState.Enabled &&
                        x.getStatus() == Status.Up)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hostsInCluster)) {
            throw new CloudRuntimeException("No host available in cluster " + cluster.getUuid() + " (" + cluster.getName() + ") to support host " +
                    host.getUuid() + " (" + host.getName() + ") in maintenance");
        }
        List<VMInstanceVO> vmsRunning = vmInstanceDao.listByHostId(host.getId());
        if (CollectionUtils.isEmpty(vmsRunning)) {
            return new Pair<>(true, "OK");
        }
        List<String> hostTags = hostTagsDao.getHostTags(host.getId());

        int successfullyCheckedVmMigrations = 0;
        for (VMInstanceVO runningVM : vmsRunning) {
            boolean canMigrateVm = false;
            ServiceOfferingVO serviceOffering = serviceOfferingDao.findById(runningVM.getServiceOfferingId());
            for (Host hostInCluster : hostsInCluster) {
                if (!checkHostTags(hostTags, hostTagsDao.getHostTags(hostInCluster.getId()), serviceOffering.getHostTag())) {
                    s_logger.debug(String.format("Host tags mismatch between %s and %s Skipping it from the capacity check", host, hostInCluster));
                    continue;
                }
                DeployDestination deployDestination = new DeployDestination(null, null, null, host);
                VirtualMachineProfileImpl vmProfile = new VirtualMachineProfileImpl(runningVM);
                boolean affinityChecks = true;
                for (AffinityGroupProcessor affinityProcessor : _affinityProcessors) {
                    affinityChecks = affinityChecks && affinityProcessor.check(vmProfile, deployDestination);
                }
                if (!affinityChecks) {
                    s_logger.debug(String.format("Affinity check failed between %s and %s Skipping it from the capacity check", host, hostInCluster));
                    continue;
                }
                boolean maxGuestLimit = capacityManager.checkIfHostReachMaxGuestLimit(host);
                boolean hostHasCPUCapacity = capacityManager.checkIfHostHasCpuCapability(hostInCluster.getId(), serviceOffering.getCpu(), serviceOffering.getSpeed());
                int cpuRequested = serviceOffering.getCpu() * serviceOffering.getSpeed();
                long ramRequested = serviceOffering.getRamSize() * 1024L * 1024L;
                ClusterDetailsVO clusterDetailsCpuOvercommit = clusterDetailsDao.findDetail(cluster.getId(), "cpuOvercommitRatio");
                ClusterDetailsVO clusterDetailsRamOvercommmt = clusterDetailsDao.findDetail(cluster.getId(), "memoryOvercommitRatio");
                Float cpuOvercommitRatio = Float.parseFloat(clusterDetailsCpuOvercommit.getValue());
                Float memoryOvercommitRatio = Float.parseFloat(clusterDetailsRamOvercommmt.getValue());
                boolean hostHasCapacity = capacityManager.checkIfHostHasCapacity(hostInCluster.getId(), cpuRequested, ramRequested, false,
                        cpuOvercommitRatio, memoryOvercommitRatio, false);
                if (!maxGuestLimit && hostHasCPUCapacity && hostHasCapacity) {
                    canMigrateVm = true;
                    break;
                }
            }
            if (!canMigrateVm) {
                String msg = String.format("%s cannot be migrated away from %s to any other host in the cluster", runningVM, host);
                s_logger.error(msg);
                return new Pair<>(false, msg);
            }
            successfullyCheckedVmMigrations++;
        }
        if (successfullyCheckedVmMigrations != vmsRunning.size()) {
            String migrationCheckDetails = String.format("%s cannot enter maintenance mode as capacity check failed for hosts in cluster %s", host, cluster);
            return new Pair<>(false, migrationCheckDetails);
        }
        return new Pair<>(true, "OK");
    }

    /**
     * Check hosts tags
     */
    private boolean checkHostTags(List<String> hostTags, List<String> hostInClusterTags, String offeringTag) {
        if (CollectionUtils.isEmpty(hostTags) && CollectionUtils.isEmpty(hostInClusterTags)) {
            return true;
        } else if ((CollectionUtils.isNotEmpty(hostTags) && CollectionUtils.isEmpty(hostInClusterTags)) ||
                (CollectionUtils.isEmpty(hostTags) && CollectionUtils.isNotEmpty(hostInClusterTags))) {
            return false;
        } else {
            return hostInClusterTags.contains(offeringTag);
        }
    }

    /**
     * Retrieve all the hosts in 'Up' state within the scope for starting rolling maintenance
     */
    protected Map<Long, List<Host>> getHostsByClusterForRollingMaintenance(ResourceType type, List<Long> ids) {
        Set<Host> hosts = new HashSet<>();
        List<HostVO> hostsInScope = null;
        for (Long id : ids) {
            if (type == ResourceType.Host) {
                hostsInScope = Collections.singletonList(hostDao.findById(id));
            } else if (type == ResourceType.Cluster) {
                hostsInScope = hostDao.findByClusterId(id);
            } else if (type == ResourceType.Pod) {
                hostsInScope = hostDao.findByPodId(id);
            } else if (type == ResourceType.Zone) {
                hostsInScope = hostDao.findByDataCenterId(id);
            }
            List<HostVO> hostsUp = hostsInScope.stream()
                    .filter(x -> x.getHypervisorType() == Hypervisor.HypervisorType.KVM)
                    .collect(Collectors.toList());
            hosts.addAll(hostsUp);
        }
        return hosts.stream().collect(Collectors.groupingBy(Host::getClusterId));
    }

    @Override
    public Pair<ResourceType, List<Long>> getResourceTypeIdPair(StartRollingMaintenanceCmd cmd) {
        return getResourceTypeAndIdPair(cmd.getPodIds(), cmd.getClusterIds(), cmd.getZoneIds(), cmd.getHostIds());
    }

    /*
        Wait for to be in maintenance mode
     */
    private void waitForHostInMaintenance(long hostId) throws CloudRuntimeException, InterruptedException {
        HostVO host = hostDao.findById(hostId);
        long timeout = KvmRollingMaintenanceWaitForMaintenanceTimeout.value() * 1000L;
        long timeSpent = 0;
        long step = 30 * 1000L;
        while (timeSpent < timeout && host.getResourceState() != ResourceState.Maintenance) {
            Thread.sleep(step);
            timeSpent += step;
            host = hostDao.findById(hostId);
        }

        if (host.getResourceState() != ResourceState.Maintenance) {
            String errorMsg = "Timeout: waited " + timeout + "ms for host " + host.getUuid() + "(" + host.getName() + ")" +
                    " to be in Maintenance state, but after timeout it is in " + host.getResourceState().toString() + " state";
            s_logger.error(errorMsg);
            throw new CloudRuntimeException(errorMsg);
        }
        s_logger.debug("Host " + host.getUuid() + "(" + host.getName() + ") is in maintenance");
    }

    @Override
    public String getConfigComponentName() {
        return RollingMaintenanceManagerImpl.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {KvmRollingMaintenanceStageTimeout, KvmRollingMaintenancePingInterval, KvmRollingMaintenanceWaitForMaintenanceTimeout};
    }
}
