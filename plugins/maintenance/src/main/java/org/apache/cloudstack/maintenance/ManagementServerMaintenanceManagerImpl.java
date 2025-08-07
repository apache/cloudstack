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

import com.cloud.resource.ResourceState;
import org.apache.cloudstack.agent.lb.IndirectAgentLB;
import org.apache.cloudstack.agent.lb.IndirectAgentLBServiceImpl;
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
import org.apache.cloudstack.management.ManagementServerHost;
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
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.host.HostVO;
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
    public boolean stop() {
        ManagementServerHostVO msHost = msHostDao.findByMsid(ManagementServerNode.getManagementServerId());
        if (msHost != null) {
            updateLastManagementServerForHosts(msHost.getMsid());
        }
        return true;
    }

    private void updateLastManagementServerForHosts(long msId) {
        List<HostVO> hosts = hostDao.listHostsByMs(msId);
        for (HostVO host : hosts) {
            if (host != null) {
                host.setLastManagementServerId(msId);
                hostDao.update(host.getId(), host);
            }
        }
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
    public void onPreparingForMaintenance() {
        synchronized (_listeners) {
            for (final ManagementServerMaintenanceListener listener : _listeners) {
                logger.info("Invoke, on preparing for maintenance for listener " + listener.getClass());
                listener.onManagementServerPreparingForMaintenance();
            }
        }
    }

    @Override
    public void onCancelPreparingForMaintenance() {
        synchronized (_listeners) {
            for (final ManagementServerMaintenanceListener listener : _listeners) {
                logger.info("Invoke, on cancel preparing for maintenance for listener " + listener.getClass());
                listener.onManagementServerCancelPreparingForMaintenance();
            }
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

    private void resetShutdownParams() {
        logger.debug("Resetting shutdown params");
        preparingForShutdown = false;
        shutdownTriggered = false;
    }

    @Override
    public boolean isPreparingForMaintenance() {
        return preparingForMaintenance;
    }

    @Override
    public void resetMaintenanceParams() {
        logger.debug("Resetting maintenance params");
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
        ManagementServerHostVO msHost = msHostDao.findByMsid(ManagementServerNode.getManagementServerId());
        if (msHost == null) {
            throw new CloudRuntimeException("Invalid node id for the management server");
        }
        msHostDao.updateState(msHost.getId(), State.ShuttingDown);
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
        waitForPendingJobs(false);
    }

    @Override
    public void prepareForShutdown() {
        prepareForShutdown(false);
        ManagementServerHostVO msHost = msHostDao.findByMsid(ManagementServerNode.getManagementServerId());
        if (msHost == null) {
            throw new CloudRuntimeException("Invalid node id for the management server");
        }
        msHostDao.updateState(msHost.getId(), State.PreparingForShutDown);
    }

    @Override
    public void cancelShutdown() {
        ManagementServerHostVO msHost = msHostDao.findByMsid(ManagementServerNode.getManagementServerId());
        if (msHost == null) {
            throw new CloudRuntimeException("Invalid node id for the management server");
        }
        if (!this.preparingForShutdown && !(State.PreparingForShutDown.equals(msHost.getState()) || State.ReadyToShutDown.equals(msHost.getState()))) {
            throw new CloudRuntimeException("Shutdown has not been triggered");
        }

        resetShutdownParams();
        resetMaintenanceParams();
        jobManager.enableAsyncJobs();
        cancelWaitForPendingJobs();
        msHostDao.updateState(msHost.getId(), State.Up);
    }

    @Override
    public void prepareForMaintenance(String lbAlorithm, boolean forced) {
        if (this.preparingForShutdown) {
            throw new CloudRuntimeException("Shutdown has already been triggered, cancel shutdown and try again");
        }

        if (this.preparingForMaintenance) {
            throw new CloudRuntimeException("Maintenance has already been initiated");
        }

        ManagementServerHostVO msHost = msHostDao.findByMsid(ManagementServerNode.getManagementServerId());
        if (msHost == null) {
            throw new CloudRuntimeException("Invalid node id for the management server");
        }
        this.preparingForMaintenance = true;
        this.maintenanceStartTime = System.currentTimeMillis();
        this.lbAlgorithm = lbAlorithm;
        jobManager.disableAsyncJobs();
        onPreparingForMaintenance();
        waitForPendingJobs(forced);
        msHostDao.updateState(msHost.getId(), State.PreparingForMaintenance);
    }

    @Override
    public void cancelMaintenance() {
        ManagementServerHostVO msHost = msHostDao.findByMsid(ManagementServerNode.getManagementServerId());
        if (msHost == null) {
            throw new CloudRuntimeException("Invalid node id for the management server");
        }
        if (!this.preparingForMaintenance && !(State.Maintenance.equals(msHost.getState()) || State.PreparingForMaintenance.equals(msHost.getState()))) {
            throw new CloudRuntimeException("Maintenance has not been initiated");
        }
        resetMaintenanceParams();
        resetShutdownParams();
        jobManager.enableAsyncJobs();
        cancelWaitForPendingJobs();
        msHostDao.updateState(msHost.getId(), State.Up);
        ScheduledExecutorService cancelMaintenanceService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("CancelMaintenance-Job"));
        cancelMaintenanceService.schedule(() -> {
            cancelMaintenanceTask(msHost.getState());
        }, 0, TimeUnit.SECONDS);
        cancelMaintenanceService.shutdown();
    }

    private void cancelMaintenanceTask(ManagementServerHost.State msState) {
        if (State.PreparingForMaintenance.equals(msState)) {
            onCancelPreparingForMaintenance();
        }
        if (State.Maintenance.equals(msState)) {
            onCancelMaintenance();
        }
    }

    private void waitForPendingJobs(boolean forceMaintenance) {
        cancelWaitForPendingJobs();
        pendingJobsCheckTask = Executors.newScheduledThreadPool(1, new NamedThreadFactory("PendingJobsCheck"));
        long pendingJobsCheckDelayInSecs = 1L; // 1 sec
        long pendingJobsCheckPeriodInSecs = 3L; // every 3 secs, check more frequently for pending jobs
        boolean ignoreMaintenanceHosts = ManagementServerMaintenanceIgnoreMaintenanceHosts.value();
        pendingJobsCheckTask.scheduleAtFixedRate(new CheckPendingJobsTask(this, ignoreMaintenanceHosts, forceMaintenance), pendingJobsCheckDelayInSecs, pendingJobsCheckPeriodInSecs, TimeUnit.SECONDS);
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
    @ActionEvent(eventType = EventTypes.EVENT_MS_SHUTDOWN_PREPARE, eventDescription = "preparing for shutdown")
    public ManagementServerMaintenanceResponse prepareForShutdown(PrepareForShutdownCmd cmd) {
        ManagementServerHostVO msHost = msHostDao.findById(cmd.getManagementServerId());
        if (msHost == null) {
            throw new CloudRuntimeException("Unable to find the management server, cannot prepare for shutdown");
        }

        if (!State.Up.equals(msHost.getState())) {
            throw new CloudRuntimeException("Management server is not in the right state to prepare for shutdown");
        }

        checkAnyMsInPreparingStates("prepare for shutdown");

        final Command[] cmds = new Command[1];
        cmds[0] = new PrepareForShutdownManagementServerHostCommand(msHost.getMsid());
        executeCmd(msHost, cmds);

        return prepareMaintenanceResponse(cmd.getManagementServerId());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_MS_SHUTDOWN, eventDescription = "triggering shutdown")
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
            checkAnyMsInPreparingStates("trigger shutdown");
            msHostDao.updateState(msHost.getId(), State.PreparingForShutDown);
        }

        final Command[] cmds = new Command[1];
        cmds[0] = new TriggerShutdownManagementServerHostCommand(msHost.getMsid());
        executeCmd(msHost, cmds);

        return prepareMaintenanceResponse(cmd.getManagementServerId());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_MS_SHUTDOWN_CANCEL, eventDescription = "cancelling shutdown")
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
        executeCmd(msHost, cmds);

        return prepareMaintenanceResponse(cmd.getManagementServerId());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_MS_MAINTENANCE_PREPARE, eventDescription = "preparing for maintenance")
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

        checkAnyMsInPreparingStates("prepare for maintenance");

        boolean ignoreMaintenanceHosts = ManagementServerMaintenanceIgnoreMaintenanceHosts.value();
        if (indirectAgentLB.haveAgentBasedHosts(msHost.getMsid(), ignoreMaintenanceHosts)) {
            List<String> indirectAgentMsList = indirectAgentLB.getManagementServerList();
            indirectAgentMsList.remove(msHost.getServiceIP());
            List<String> nonUpMsList = msHostDao.listNonUpStateMsIPs();
            indirectAgentMsList.removeAll(nonUpMsList);
            if (CollectionUtils.isEmpty(indirectAgentMsList)) {
                throw new CloudRuntimeException(String.format("Cannot prepare for maintenance, no other active management servers found from '%s' setting", ApiServiceConfiguration.ManagementServerAddresses.key()));
            }
        }

        final Command[] cmds = new Command[1];
        cmds[0] = new PrepareForMaintenanceManagementServerHostCommand(msHost.getMsid(), cmd.getAlgorithm(), cmd.isForced());
        executeCmd(msHost, cmds);

        return prepareMaintenanceResponse(cmd.getManagementServerId());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_MS_MAINTENANCE_CANCEL, eventDescription = "cancelling maintenance")
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
        executeCmd(msHost, cmds);

        if (cmd.getRebalance()) {
            logger.info("Propagate MS list and rebalance indirect agents");
            indirectAgentLB.propagateMSListToAgents(true);
        }

        return prepareMaintenanceResponse(cmd.getManagementServerId());
    }

    private void executeCmd(ManagementServerHostVO msHost, Command[] cmds) {
        if (msHost == null) {
            throw new CloudRuntimeException("Management server node not specified, to execute the cmd");
        }
        if (cmds == null || cmds.length <= 0) {
            throw new CloudRuntimeException(String.format("Cmd not specified, to execute on the management server node %s", msHost));
        }
        String result = clusterManager.execute(String.valueOf(msHost.getMsid()), 0, gson.toJson(cmds), false);
        if (result == null) {
            String msg = String.format("Unable to reach or execute %s on the management server node: %s", cmds[0], msHost);
            logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }
        logger.info(String.format("Cmd %s - result: %s", cmds[0], result));
        if (!result.startsWith("Success")) {
            throw new CloudRuntimeException(result);
        }
    }

    @Override
    public void cancelPreparingForMaintenance(ManagementServerHostVO msHost) {
        resetMaintenanceParams();
        resetShutdownParams();
        jobManager.enableAsyncJobs();
        if (msHost == null) {
            msHost = msHostDao.findByMsid(ManagementServerNode.getManagementServerId());
            if (msHost == null) {
                throw new CloudRuntimeException("Invalid node id for the management server");
            }
        }
        onCancelPreparingForMaintenance();
        msHostDao.updateState(msHost.getId(), State.Up);
    }

    private void checkAnyMsInPreparingStates(String operation) {
        final List<ManagementServerHostVO> preparingForMaintenanceOrShutDownMsList = msHostDao.listBy(State.PreparingForMaintenance, State.PreparingForShutDown);
        if (CollectionUtils.isNotEmpty(preparingForMaintenanceOrShutDownMsList)) {
            throw new CloudRuntimeException(String.format("Cannot %s, there are other management servers preparing for maintenance/shutdown", operation));
        }
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
        List<String> agents = hostDao.listByMs(msHost.getMsid());
        long agentsCount = agents.size();
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
                ManagementServerMaintenanceTimeoutInMins, ManagementServerMaintenanceIgnoreMaintenanceHosts
        };
    }

    private final class CheckPendingJobsTask extends ManagedContextRunnable {

        private ManagementServerMaintenanceManager managementServerMaintenanceManager;
        private boolean ignoreMaintenanceHosts = false;
        private boolean agentsTransferTriggered = false;
        private boolean forceMaintenance = false;

        public CheckPendingJobsTask(ManagementServerMaintenanceManager managementServerMaintenanceManager, boolean ignoreMaintenanceHosts, boolean forceMaintenance) {
            this.managementServerMaintenanceManager = managementServerMaintenanceManager;
            this.ignoreMaintenanceHosts = ignoreMaintenanceHosts;
            this.forceMaintenance = forceMaintenance;
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
                    if (forceMaintenance) {
                        logger.debug("Maintenance window timeout, MS is forced to Maintenance Mode");
                        ManagementServerHostVO msHost = msHostDao.findByMsid(ManagementServerNode.getManagementServerId());
                        if (msHost == null) {
                            logger.warn("Unable to find the management server, invalid node id");
                            managementServerMaintenanceManager.cancelWaitForPendingJobs();
                            return;
                        }
                        msHostDao.updateState(msHost.getId(), State.Maintenance);
                        managementServerMaintenanceManager.onMaintenance();
                        managementServerMaintenanceManager.cancelWaitForPendingJobs();
                        return;
                    }

                    logger.debug("Maintenance window timeout, terminating the pending jobs check timer task");
                    managementServerMaintenanceManager.cancelPreparingForMaintenance(null);
                    managementServerMaintenanceManager.cancelWaitForPendingJobs();
                    return;
                }

                long totalPendingJobs = managementServerMaintenanceManager.countPendingJobs(ManagementServerNode.getManagementServerId());

                long totalAgents = totalAgentsInMs();

                String msg = String.format("Checking for triggered maintenance or shutdown... shutdownTriggered [%b] preparingForShutdown[%b] preparingForMaintenance[%b] AllowAsyncJobs [%b] PendingJobCount [%d] AgentsCount [%d]",
                        managementServerMaintenanceManager.isShutdownTriggered(), managementServerMaintenanceManager.isPreparingForShutdown(), managementServerMaintenanceManager.isPreparingForMaintenance(), managementServerMaintenanceManager.isAsyncJobsEnabled(), totalPendingJobs, totalAgents);
                logger.debug(msg);

                if (totalPendingJobs > 0) {
                    logger.info(String.format("There are %d pending jobs, trying again later", totalPendingJobs));
                    return;
                }

                // No more pending jobs. Good to terminate
                if (managementServerMaintenanceManager.isShutdownTriggered()) {
                    logger.info("MS is Shutting Down Now");
                    System.exit(0);
                }
                if (managementServerMaintenanceManager.isPreparingForMaintenance()) {
                    ManagementServerHostVO msHost = msHostDao.findByMsid(ManagementServerNode.getManagementServerId());
                    if (msHost == null) {
                        logger.warn("Unable to find the management server, invalid node id");
                        managementServerMaintenanceManager.cancelWaitForPendingJobs();
                        return;
                    }
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
                    boolean agentsMigrated = indirectAgentLB.migrateAgents(msHost.getUuid(), ManagementServerNode.getManagementServerId(), managementServerMaintenanceManager.getLbAlgorithm(), remainingMaintenanceWindowInMs(), ignoreMaintenanceHosts);
                    if (!agentsMigrated) {
                        logger.warn(String.format("Unable to prepare for maintenance, cannot migrate indirect agents on this management server node %d (id: %s)", ManagementServerNode.getManagementServerId(), msHost.getUuid()));
                        managementServerMaintenanceManager.cancelPreparingForMaintenance(msHost);
                        managementServerMaintenanceManager.cancelWaitForPendingJobs();
                        return;
                    }

                    if(!agentMgr.transferDirectAgentsFromMS(msHost.getUuid(), ManagementServerNode.getManagementServerId(), remainingMaintenanceWindowInMs(), ignoreMaintenanceHosts)) {
                        logger.warn(String.format("Unable to prepare for maintenance, cannot transfer direct agents on this management server node %d (id: %s)", ManagementServerNode.getManagementServerId(), msHost.getUuid()));
                        managementServerMaintenanceManager.cancelPreparingForMaintenance(msHost);
                        managementServerMaintenanceManager.cancelWaitForPendingJobs();
                    }
                } else if (managementServerMaintenanceManager.isPreparingForShutdown()) {
                    logger.info("MS is Ready To Shutdown");
                    ManagementServerHostVO msHost = msHostDao.findByMsid(ManagementServerNode.getManagementServerId());
                    if (msHost == null) {
                        logger.warn("Unable to find the management server, invalid node id");
                        managementServerMaintenanceManager.cancelWaitForPendingJobs();
                        return;
                    }
                    msHostDao.updateState(msHost.getId(), State.ReadyToShutDown);
                    managementServerMaintenanceManager.cancelWaitForPendingJobs();
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

        private long totalAgentsInMs() {
            /* Any Host in Maintenance state could block moving Management Server to Maintenance state, exclude those Hosts from total agents count
             * To exclude maintenance states use values from ResourceState as source of truth
             */
            List<ResourceState> statesToExclude = ignoreMaintenanceHosts ? ResourceState.s_maintenanceStates : List.of();
            return hostDao.countHostsByMsResourceStateTypeAndHypervisorType(ManagementServerNode.getManagementServerId(), statesToExclude,
                    IndirectAgentLBServiceImpl.agentValidHostTypes, null);
        }
    }
}
