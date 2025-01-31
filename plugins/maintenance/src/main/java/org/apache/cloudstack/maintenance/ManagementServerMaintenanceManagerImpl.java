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

package org.apache.cloudstack.maintenance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.cloudstack.agent.lb.IndirectAgentLB;
import org.apache.cloudstack.api.command.CancelMaintenanceCmd;
import org.apache.cloudstack.api.command.CancelShutdownCmd;
import org.apache.cloudstack.api.command.PrepareForMaintenanceCmd;
import org.apache.cloudstack.api.command.PrepareForShutdownCmd;
import org.apache.cloudstack.api.command.ReadyForShutdownCmd;
import org.apache.cloudstack.api.command.TriggerShutdownCmd;
import org.apache.cloudstack.api.response.ManagementServerMaintenanceResponse;
import org.apache.cloudstack.config.ApiServiceConfiguration;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.management.ManagementServerHost.State;
import org.apache.cloudstack.maintenance.command.CancelMaintenanceManagementServerHostCommand;
import org.apache.cloudstack.maintenance.command.CancelShutdownManagementServerHostCommand;
import org.apache.cloudstack.maintenance.command.PrepareForMaintenanceManagementServerHostCommand;
import org.apache.cloudstack.maintenance.command.PrepareForShutdownManagementServerHostCommand;
import org.apache.cloudstack.maintenance.command.TriggerShutdownManagementServerHostCommand;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.commons.collections.CollectionUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Command;
import com.cloud.cluster.ClusterManager;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.host.dao.HostDao;
import com.cloud.serializer.GsonHelper;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.Gson;

public class ManagementServerMaintenanceManagerImpl extends ManagerBase implements ManagementServerMaintenanceManager, PluggableService, Configurable {

    Gson gson;

    @Inject
    private AsyncJobManager jobManager;
    @Inject
    private ClusterManager clusterManager;
    @Inject
    private AgentManager agentMgr;
    @Inject
    private IndirectAgentLB indirectAgentLB;
    @Inject
    private ManagementServerHostDao msHostDao;
    @Inject
    private HostDao hostDao;

    private final List<ManagementServerMaintenanceListener> _listeners = new ArrayList<>();

    private boolean shutdownTriggered = false;
    private boolean preparingForShutdown = false;
    private boolean preparingForMaintenance = false;
    private long maintenanceStartTime = 0;
    private String lbAlgorithm;

    private ScheduledExecutorService pendingJobsCheckTask;

    protected ManagementServerMaintenanceManagerImpl() {
        super();
        gson = GsonHelper.getGson();
    }

    @Override
    public boolean start() {
        ManagementServerHostVO msHost = msHostDao.findByMsid(ManagementServerNode.getManagementServerId());
        if (msHost != null) {
            State[] maintenanceStates = {State.PreparingForMaintenance, State.Maintenance};
            if (Arrays.asList(maintenanceStates).contains(msHost.getState())) {
                this.preparingForMaintenance = true;
                jobManager.disableAsyncJobs();
                msHostDao.updateState(msHost.getId(), State.Maintenance);
            }
        }
        return true;
    }

    @Override
    public void registerListener(ManagementServerMaintenanceListener listener) {
        synchronized (_listeners) {
            logger.info("Register management server maintenance listener " + listener.getClass());
            _listeners.add(listener);
        }
    }

    @Override
    public void unregisterListener(ManagementServerMaintenanceListener listener) {
        synchronized (_listeners) {
            logger.info("Unregister management server maintenance listener " + listener.getClass());
            _listeners.remove(listener);
        }
    }

    @Override
    public void onMaintenance() {
        synchronized (_listeners) {
            for (final ManagementServerMaintenanceListener listener : _listeners) {
                logger.info("Invoke, on maintenance for listener " + listener.getClass());
                listener.onManagementServerMaintenance();
            }
        }
    }

    @Override
    public void onCancelMaintenance() {
        synchronized (_listeners) {
            for (final ManagementServerMaintenanceListener listener : _listeners) {
                logger.info("Invoke, on cancel maintenance for listener " + listener.getClass());
                listener.onManagementServerCancelMaintenance();
            }
        }
    }

    @Override
    public boolean isShutdownTriggered() {
        return shutdownTriggered;
    }

    @Override
    public boolean isPreparingForShutdown() {
        return preparingForShutdown;
    }

    @Override
    public boolean isPreparingForMaintenance() {
        return preparingForMaintenance;
    }

    @Override
    public void resetPreparingForMaintenance() {
        preparingForMaintenance = false;
        maintenanceStartTime = 0;
        lbAlgorithm = null;
    }

    @Override
    public long getMaintenanceStartTime() {
        return maintenanceStartTime;
    }

    @Override
    public String getLbAlgorithm() {
        return lbAlgorithm;
    }

    @Override
    public long countPendingJobs(Long... msIds) {
        return jobManager.countPendingNonPseudoJobs(msIds);
    }

    @Override
    public boolean isAsyncJobsEnabled() {
        return jobManager.isAsyncJobsEnabled();
    }

    @Override
    public void triggerShutdown() {
        if (this.shutdownTriggered) {
            throw new CloudRuntimeException("Shutdown has already been triggered");
        }
        this.shutdownTriggered = true;
        prepareForShutdown(true);
    }

    private void prepareForShutdown(boolean postTrigger) {
        if (!postTrigger) {
            if (this.preparingForMaintenance) {
                throw new CloudRuntimeException("Maintenance has already been initiated, cancel maintenance and try again");
            }

            // Ensure we don't throw an error if triggering a shutdown after just preparing for it
            if (this.preparingForShutdown) {
                throw new CloudRuntimeException("Shutdown has already been triggered");
            }
        }

        this.preparingForShutdown = true;
        jobManager.disableAsyncJobs();
        waitForPendingJobs();
    }

    @Override
    public void prepareForShutdown() {
        prepareForShutdown(false);
    }

    @Override
    public void cancelShutdown() {
        if (!this.preparingForShutdown) {
            throw new CloudRuntimeException("Shutdown has not been triggered");
        }

        this.preparingForShutdown = false;
        this.shutdownTriggered = false;
        resetPreparingForMaintenance();
        jobManager.enableAsyncJobs();
        cancelWaitForPendingJobs();
    }

    @Override
    public void prepareForMaintenance(String lbAlorithm) {
        if (this.preparingForShutdown) {
            throw new CloudRuntimeException("Shutdown has already been triggered, cancel shutdown and try again");
        }

        if (this.preparingForMaintenance) {
            throw new CloudRuntimeException("Maintenance has already been initiated");
        }
        this.preparingForMaintenance = true;
        this.maintenanceStartTime = System.currentTimeMillis();
        this.lbAlgorithm = lbAlorithm;
        jobManager.disableAsyncJobs();
        waitForPendingJobs();
    }

    @Override
    public void cancelMaintenance() {
        if (!this.preparingForMaintenance) {
            throw new CloudRuntimeException("Maintenance has not been initiated");
        }
        resetPreparingForMaintenance();
        this.preparingForShutdown = false;
        this.shutdownTriggered = false;
        jobManager.enableAsyncJobs();
        cancelWaitForPendingJobs();
        ManagementServerHostVO msHost = msHostDao.findByMsid(ManagementServerNode.getManagementServerId());
        if (msHost != null && State.Maintenance.equals(msHost.getState())) {
            onCancelMaintenance();
        }
    }

    private void waitForPendingJobs() {
        cancelWaitForPendingJobs();
        pendingJobsCheckTask = Executors.newScheduledThreadPool(1, new NamedThreadFactory("PendingJobsCheck"));
        long pendingJobsCheckDelayInSecs = 1L; // 1 sec
        long pendingJobsCheckPeriodInSecs = 3L; // every 3 secs, check more frequently for pending jobs
        pendingJobsCheckTask.scheduleAtFixedRate(new CheckPendingJobsTask(this), pendingJobsCheckDelayInSecs, pendingJobsCheckPeriodInSecs, TimeUnit.SECONDS);
    }

    @Override
    public void cancelWaitForPendingJobs() {
        if (pendingJobsCheckTask != null) {
            pendingJobsCheckTask.shutdown();
            pendingJobsCheckTask = null;
        }
    }

    @Override
    public ManagementServerMaintenanceResponse readyForShutdown(ReadyForShutdownCmd cmd) {
        return prepareMaintenanceResponse(cmd.getManagementServerId());
    }

    @Override
    public ManagementServerMaintenanceResponse prepareForShutdown(PrepareForShutdownCmd cmd) {
        ManagementServerHostVO msHost = msHostDao.findById(cmd.getManagementServerId());
        if (msHost == null) {
            throw new CloudRuntimeException("Unable to find the management server, cannot prepare for shutdown");
        }

        if (!State.Up.equals(msHost.getState())) {
            throw new CloudRuntimeException("Management server is not in the right state to prepare for shutdown");
        }

        final Command[] cmds = new Command[1];
        cmds[0] = new PrepareForShutdownManagementServerHostCommand(msHost.getMsid());
        String result = clusterManager.execute(String.valueOf(msHost.getMsid()), 0, gson.toJson(cmds), true);
        logger.info("PrepareForShutdownCmd result : " + result);
        if (!result.startsWith("Success")) {
            throw new CloudRuntimeException(result);
        }

        msHostDao.updateState(msHost.getId(), State.PreparingForShutDown);
        return prepareMaintenanceResponse(cmd.getManagementServerId());
    }

    @Override
    public ManagementServerMaintenanceResponse triggerShutdown(TriggerShutdownCmd cmd) {
        ManagementServerHostVO msHost = msHostDao.findById(cmd.getManagementServerId());
        if (msHost == null) {
            throw new CloudRuntimeException("Unable to find the management server, cannot trigger shutdown");
        }

        if (!(State.Up.equals(msHost.getState()) || State.Maintenance.equals(msHost.getState()) || State.PreparingForShutDown.equals(msHost.getState()) ||
                State.ReadyToShutDown.equals(msHost.getState()))) {
            throw new CloudRuntimeException("Management server is not in the right state to trigger shutdown");
        }

        if (State.Up.equals(msHost.getState())) {
            msHostDao.updateState(msHost.getId(), State.PreparingForShutDown);
        }

        final Command[] cmds = new Command[1];
        cmds[0] = new TriggerShutdownManagementServerHostCommand(msHost.getMsid());
        String result = clusterManager.execute(String.valueOf(msHost.getMsid()), 0, gson.toJson(cmds), true);
        logger.info("TriggerShutdownCmd result : " + result);
        if (!result.startsWith("Success")) {
            throw new CloudRuntimeException(result);
        }

        msHostDao.updateState(msHost.getId(), State.ShuttingDown);
        return prepareMaintenanceResponse(cmd.getManagementServerId());
    }

    @Override
    public ManagementServerMaintenanceResponse cancelShutdown(CancelShutdownCmd cmd) {
        ManagementServerHostVO msHost = msHostDao.findById(cmd.getManagementServerId());
        if (msHost == null) {
            throw new CloudRuntimeException("Unable to find the management server, cannot cancel shutdown");
        }

        if (!(State.PreparingForShutDown.equals(msHost.getState()) || State.ReadyToShutDown.equals(msHost.getState()))) {
            throw new CloudRuntimeException("Management server is not in the right state to cancel shutdown");
        }

        final Command[] cmds = new Command[1];
        cmds[0] = new CancelShutdownManagementServerHostCommand(msHost.getMsid());
        String result = clusterManager.execute(String.valueOf(msHost.getMsid()), 0, gson.toJson(cmds), true);
        logger.info("CancelShutdownCmd result : " + result);
        if (!result.startsWith("Success")) {
            throw new CloudRuntimeException(result);
        }

        msHostDao.updateState(msHost.getId(), State.Up);
        return prepareMaintenanceResponse(cmd.getManagementServerId());
    }

    @Override
    public ManagementServerMaintenanceResponse prepareForMaintenance(PrepareForMaintenanceCmd cmd) {
        if (StringUtils.isNotBlank(cmd.getAlgorithm())) {
            indirectAgentLB.checkLBAlgorithmName(cmd.getAlgorithm());
        }

        final List<ManagementServerHostVO> activeMsList = msHostDao.listBy(State.Up);
        if (CollectionUtils.isEmpty(activeMsList)) {
            throw new CloudRuntimeException("Cannot prepare for maintenance, no active management servers found");
        }

        if (activeMsList.size() == 1) {
            throw new CloudRuntimeException("Prepare for maintenance not supported, there is only one active management server");
        }

        ManagementServerHostVO msHost = msHostDao.findById(cmd.getManagementServerId());
        if (msHost == null) {
            throw new CloudRuntimeException("Cannot prepare for maintenance, unable to find the management server");
        }

        if (!State.Up.equals(msHost.getState())) {
            throw new CloudRuntimeException("Management server is not in the right state to prepare for maintenance");
        }

        final List<ManagementServerHostVO> preparingForMaintenanceMsList = msHostDao.listBy(State.PreparingForMaintenance);
        if (CollectionUtils.isNotEmpty(preparingForMaintenanceMsList)) {
            throw new CloudRuntimeException("Cannot prepare for maintenance, there are other management servers preparing for maintenance");
        }

        if (indirectAgentLB.haveAgentBasedHosts(msHost.getMsid())) {
            List<String> indirectAgentMsList = indirectAgentLB.getManagementServerList();
            indirectAgentMsList.remove(msHost.getServiceIP());
            List<String> nonUpMsList = msHostDao.listNonUpStateMsIPs();
            indirectAgentMsList.removeAll(nonUpMsList);
            if (CollectionUtils.isEmpty(indirectAgentMsList)) {
                throw new CloudRuntimeException(String.format("Cannot prepare for maintenance, no other active management servers found from '%s' setting", ApiServiceConfiguration.ManagementServerAddresses.key()));
            }
        }

        List<String> lastAgents = hostDao.listByMs(cmd.getManagementServerId());
        agentMgr.setLastAgents(lastAgents);

        final Command[] cmds = new Command[1];
        cmds[0] = new PrepareForMaintenanceManagementServerHostCommand(msHost.getMsid(), cmd.getAlgorithm());
        String result = clusterManager.execute(String.valueOf(msHost.getMsid()), 0, gson.toJson(cmds), true);
        logger.info("PrepareForMaintenanceCmd result : " + result);
        if (!result.startsWith("Success")) {
            agentMgr.setLastAgents(null);
            throw new CloudRuntimeException(result);
        }

        msHostDao.updateState(msHost.getId(), State.PreparingForMaintenance);
        return prepareMaintenanceResponse(cmd.getManagementServerId());
    }

    @Override
    public ManagementServerMaintenanceResponse cancelMaintenance(CancelMaintenanceCmd cmd) {
        ManagementServerHostVO msHost = msHostDao.findById(cmd.getManagementServerId());
        if (msHost == null) {
            throw new CloudRuntimeException("Unable to find the management server, cannot cancel maintenance");
        }

        if (!(State.Maintenance.equals(msHost.getState()) || State.PreparingForMaintenance.equals(msHost.getState()))) {
            throw new CloudRuntimeException("Management server is not in the right state to cancel maintenance");
        }

        final Command[] cmds = new Command[1];
        cmds[0] = new CancelMaintenanceManagementServerHostCommand(msHost.getMsid());
        String result = clusterManager.execute(String.valueOf(msHost.getMsid()), 0, gson.toJson(cmds), true);
        logger.info("CancelMaintenanceCmd result : " + result);
        if (!result.startsWith("Success")) {
            throw new CloudRuntimeException(result);
        }

        msHostDao.updateState(msHost.getId(), State.Up);
        agentMgr.setLastAgents(null);
        return prepareMaintenanceResponse(cmd.getManagementServerId());
    }

    @Override
    public void cancelPreparingForMaintenance(ManagementServerHostVO msHost) {
        resetPreparingForMaintenance();
        this.preparingForShutdown = false;
        this.shutdownTriggered = false;
        jobManager.enableAsyncJobs();
        if (msHost == null) {
            msHost = msHostDao.findByMsid(ManagementServerNode.getManagementServerId());
        }
        msHostDao.updateState(msHost.getId(), State.Up);
    }

    private ManagementServerMaintenanceResponse prepareMaintenanceResponse(Long managementServerId) {
        ManagementServerHostVO msHost;
        Long[] msIds;
        if (managementServerId == null) {
            msHost = msHostDao.findByMsid(ManagementServerNode.getManagementServerId());
        } else {
            msHost = msHostDao.findById(managementServerId);
        }
        if (msHost == null) {
            throw new CloudRuntimeException("Unable to find the management server");
        }

        State[] maintenanceStates = {State.PreparingForMaintenance, State.Maintenance};
        State[] shutdownStates = {State.ShuttingDown, State.PreparingForShutDown, State.ReadyToShutDown};
        boolean maintenanceInitiatedForMS = Arrays.asList(maintenanceStates).contains(msHost.getState());
        boolean shutdownTriggeredForMS = Arrays.asList(shutdownStates).contains(msHost.getState());
        msIds = new Long[]{msHost.getMsid()};
        List<String> agents = hostDao.listByMs(managementServerId);
        long agentsCount = hostDao.countByMs(managementServerId);
        long pendingJobCount = countPendingJobs(msIds);
        return new ManagementServerMaintenanceResponse(msHost.getUuid(), msHost.getState(), maintenanceInitiatedForMS, shutdownTriggeredForMS,  pendingJobCount == 0, pendingJobCount, agentsCount, agents);
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(PrepareForMaintenanceCmd.class);
        cmdList.add(CancelMaintenanceCmd.class);
        cmdList.add(PrepareForShutdownCmd.class);
        cmdList.add(CancelShutdownCmd.class);
        cmdList.add(ReadyForShutdownCmd.class);
        cmdList.add(TriggerShutdownCmd.class);
        return cmdList;
    }

    @Override
    public String getConfigComponentName() {
        return ManagementServerMaintenanceManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{
                ManagementServerMaintenanceTimeoutInMins
        };
    }

    private final class CheckPendingJobsTask extends ManagedContextRunnable {

        private ManagementServerMaintenanceManager managementServerMaintenanceManager;
        private boolean agentsTransferTriggered = false;

        public CheckPendingJobsTask(ManagementServerMaintenanceManager managementServerMaintenanceManager) {
            this.managementServerMaintenanceManager = managementServerMaintenanceManager;
        }

        @Override
        protected void runInContext() {
            try {
                // If the maintenance or shutdown has been cancelled
                if (!(managementServerMaintenanceManager.isPreparingForMaintenance() || managementServerMaintenanceManager.isPreparingForShutdown())) {
                    logger.info("Maintenance/Shutdown cancelled, terminating the pending jobs check timer task");
                    managementServerMaintenanceManager.cancelWaitForPendingJobs();
                    return;
                }

                if (managementServerMaintenanceManager.isPreparingForMaintenance() && isMaintenanceWindowExpired()) {
                    logger.debug("Maintenance window timeout, terminating the pending jobs check timer task");
                    managementServerMaintenanceManager.cancelPreparingForMaintenance(null);
                    managementServerMaintenanceManager.cancelWaitForPendingJobs();
                    return;
                }

                long totalPendingJobs = managementServerMaintenanceManager.countPendingJobs(ManagementServerNode.getManagementServerId());
                int totalAgents = hostDao.countByMs(ManagementServerNode.getManagementServerId());
                String msg = String.format("Checking for triggered maintenance or shutdown... shutdownTriggered [%b] AllowAsyncJobs [%b] PendingJobCount [%d] AgentsCount [%d]",
                        managementServerMaintenanceManager.isShutdownTriggered(), managementServerMaintenanceManager.isAsyncJobsEnabled(), totalPendingJobs, totalAgents);
                logger.debug(msg);

                if (totalPendingJobs > 0) {
                    logger.info(String.format("There are %d pending jobs, trying again later", totalPendingJobs));
                    return;
                }

                // No more pending jobs. Good to terminate
                if (managementServerMaintenanceManager.isShutdownTriggered()) {
                    logger.info("MS is Shutting Down Now");
                    // update state to down ?
                    System.exit(0);
                }
                if (managementServerMaintenanceManager.isPreparingForMaintenance()) {
                    ManagementServerHostVO msHost = msHostDao.findByMsid(ManagementServerNode.getManagementServerId());
                    if (totalAgents == 0) {
                        logger.info("MS is in Maintenance Mode");
                        msHostDao.updateState(msHost.getId(), State.Maintenance);
                        managementServerMaintenanceManager.onMaintenance();
                        managementServerMaintenanceManager.cancelWaitForPendingJobs();
                        return;
                    }

                    if (agentsTransferTriggered) {
                        logger.info(String.format("There are %d agents, trying again later", totalAgents));
                        return;
                    }

                    agentsTransferTriggered = true;
                    logger.info(String.format("Preparing for maintenance - migrating agents from management server node %d (id: %s)", ManagementServerNode.getManagementServerId(), msHost.getUuid()));
                    boolean agentsMigrated = indirectAgentLB.migrateAgents(msHost.getUuid(), ManagementServerNode.getManagementServerId(), managementServerMaintenanceManager.getLbAlgorithm(), remainingMaintenanceWindowInMs());
                    if (!agentsMigrated) {
                        logger.warn(String.format("Unable to prepare for maintenance, cannot migrate indirect agents on this management server node %d (id: %s)", ManagementServerNode.getManagementServerId(), msHost.getUuid()));
                        managementServerMaintenanceManager.cancelPreparingForMaintenance(msHost);
                        managementServerMaintenanceManager.cancelWaitForPendingJobs();
                        return;
                    }

                    if(!agentMgr.transferDirectAgentsFromMS(msHost.getUuid(), ManagementServerNode.getManagementServerId(), remainingMaintenanceWindowInMs())) {
                        logger.warn(String.format("Unable to prepare for maintenance, cannot transfer direct agents on this management server node %d (id: %s)", ManagementServerNode.getManagementServerId(), msHost.getUuid()));
                        managementServerMaintenanceManager.cancelPreparingForMaintenance(msHost);
                        managementServerMaintenanceManager.cancelWaitForPendingJobs();
                        return;
                    }
                } else if (managementServerMaintenanceManager.isPreparingForShutdown()) {
                    logger.info("MS is Ready To Shutdown");
                    ManagementServerHostVO msHost = msHostDao.findByMsid(ManagementServerNode.getManagementServerId());
                    msHostDao.updateState(msHost.getId(), State.ReadyToShutDown);
                    managementServerMaintenanceManager.cancelWaitForPendingJobs();
                    return;
                }
            } catch (final Exception e) {
                logger.error("Error trying to check/run pending jobs task", e);
            }
        }

        private boolean isMaintenanceWindowExpired() {
            long maintenanceElapsedTimeInMs = System.currentTimeMillis() - managementServerMaintenanceManager.getMaintenanceStartTime();
            if (maintenanceElapsedTimeInMs >= (ManagementServerMaintenanceTimeoutInMins.value().longValue() * 60 * 1000)) {
                return true;
            }
            return false;
        }

        private long remainingMaintenanceWindowInMs() {
            long maintenanceElapsedTimeInMs = System.currentTimeMillis() - managementServerMaintenanceManager.getMaintenanceStartTime();
            long remainingMaintenanceWindowTimeInMs = (ManagementServerMaintenanceTimeoutInMins.value().longValue() * 60 * 1000) - maintenanceElapsedTimeInMs;
            return (remainingMaintenanceWindowTimeInMs > 0) ? remainingMaintenanceWindowTimeInMs : 0;
        }
    }
}
