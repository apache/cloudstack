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

package org.apache.cloudstack.shutdown;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

import org.apache.cloudstack.api.command.CancelShutdownCmd;
import org.apache.cloudstack.api.command.PrepareForShutdownCmd;
import org.apache.cloudstack.api.command.ReadyForShutdownCmd;
import org.apache.cloudstack.api.command.TriggerShutdownCmd;
import org.apache.cloudstack.api.response.ReadyForShutdownResponse;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.management.ManagementServerHost.State;
import org.apache.cloudstack.shutdown.command.CancelShutdownManagementServerHostCommand;
import org.apache.cloudstack.shutdown.command.PrepareForShutdownManagementServerHostCommand;
import org.apache.cloudstack.shutdown.command.TriggerShutdownManagementServerHostCommand;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Command;
import com.cloud.cluster.ClusterManager;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.serializer.GsonHelper;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.Gson;

public class ShutdownManagerImpl extends ManagerBase implements ShutdownManager, PluggableService{

    private static Logger logger = Logger.getLogger(ShutdownManagerImpl.class);
    Gson gson;

    @Inject
    private AsyncJobManager jobManager;
    @Inject
    private ManagementServerHostDao msHostDao;
    @Inject
    private ClusterManager clusterManager;

    private boolean shutdownTriggered = false;
    private boolean preparingForShutdown = false;

    private Timer timer = new Timer();
    private TimerTask shutdownTask;

    protected ShutdownManagerImpl() {
        super();
        gson = GsonHelper.getGson();
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
    public long countPendingJobs(Long... msIds) {
        return jobManager.countPendingNonPseudoJobs(msIds);
    }

    @Override
    public void triggerShutdown() {
        if (this.shutdownTriggered) {
            throw new CloudRuntimeException("A shutdown has already been triggered");
        }
        this.shutdownTriggered = true;
        prepareForShutdown(true);
    }

    private void prepareForShutdown(boolean postTrigger) {
        // Ensure we don't throw an error if triggering a shutdown after just preparing for it
        if (!postTrigger && this.preparingForShutdown) {
            throw new CloudRuntimeException("A shutdown has already been triggered");
        }
        this.preparingForShutdown = true;
        jobManager.disableAsyncJobs();
        if (this.shutdownTask != null) {
            this.shutdownTask.cancel();
            this.shutdownTask = null;
        }
        this.shutdownTask = new ShutdownTask(this);
        timer.scheduleAtFixedRate(shutdownTask, 0, 30L * 1000);
    }

    @Override
    public void prepareForShutdown() {
        prepareForShutdown(false);
    }

    @Override
    public void cancelShutdown() {
        if (!this.preparingForShutdown) {
            throw new CloudRuntimeException("A shutdown has not been triggered");
        }

        this.preparingForShutdown = false;
        this.shutdownTriggered = false;
        jobManager.enableAsyncJobs();
        if (shutdownTask != null) {
            shutdownTask.cancel();
        }
        shutdownTask = null;
    }

    @Override
    public ReadyForShutdownResponse readyForShutdown(Long managementserverid) {
        Long[] msIds = null;
        boolean shutdownTriggeredAnywhere = false;
        State[] shutdownTriggeredStates = {State.ShuttingDown, State.PreparingToShutDown, State.ReadyToShutDown};
        if (managementserverid == null) {
            List<ManagementServerHostVO> msHosts = msHostDao.listBy(shutdownTriggeredStates);
            if (msHosts != null && !msHosts.isEmpty()) {
                msIds = new Long[msHosts.size()];
                for (int i = 0; i < msHosts.size(); i++) {
                    msIds[i] = msHosts.get(i).getMsid();
                }
                shutdownTriggeredAnywhere = !msHosts.isEmpty();
            }
        } else {
            ManagementServerHostVO msHost = msHostDao.findById(managementserverid);
            msIds = new Long[]{msHost.getMsid()};
            shutdownTriggeredAnywhere = Arrays.asList(shutdownTriggeredStates).contains(msHost.getState());
        }
        long pendingJobCount = countPendingJobs(msIds);
        return new ReadyForShutdownResponse(managementserverid, shutdownTriggeredAnywhere, pendingJobCount == 0, pendingJobCount);
    }

    @Override
    public ReadyForShutdownResponse readyForShutdown(ReadyForShutdownCmd cmd) {
        return readyForShutdown(cmd.getManagementServerId());
    }

    @Override
    public ReadyForShutdownResponse prepareForShutdown(PrepareForShutdownCmd cmd) {
        ManagementServerHostVO msHost = msHostDao.findById(cmd.getManagementServerId());
        final Command[] cmds = new Command[1];
        cmds[0] = new PrepareForShutdownManagementServerHostCommand(msHost.getMsid());
        String result = clusterManager.execute(String.valueOf(msHost.getMsid()), 0, gson.toJson(cmds), true);
        logger.info("PrepareForShutdownCmd result : " + result);
        if (!result.contains("Success")) {
            throw new CloudRuntimeException(result);
        }

        msHost.setState(State.PreparingToShutDown);
        msHostDao.persist(msHost);

        return readyForShutdown(cmd.getManagementServerId());
    }

    @Override
    public ReadyForShutdownResponse triggerShutdown(TriggerShutdownCmd cmd) {
        ManagementServerHostVO msHost = msHostDao.findById(cmd.getManagementServerId());
        final Command[] cmds = new Command[1];
        cmds[0] = new TriggerShutdownManagementServerHostCommand(msHost.getMsid());
        String result = clusterManager.execute(String.valueOf(msHost.getMsid()), 0, gson.toJson(cmds), true);
        logger.info("TriggerShutdownCmd result : " + result);
        if (!result.contains("Success")) {
            throw new CloudRuntimeException(result);
        }

        msHost.setState(State.ShuttingDown);
        msHostDao.persist(msHost);

        return readyForShutdown(cmd.getManagementServerId());
    }

    @Override
    public ReadyForShutdownResponse cancelShutdown(CancelShutdownCmd cmd) {
        ManagementServerHostVO msHost = msHostDao.findById(cmd.getManagementServerId());
        final Command[] cmds = new Command[1];
        cmds[0] = new CancelShutdownManagementServerHostCommand(msHost.getMsid());
        String result = clusterManager.execute(String.valueOf(msHost.getMsid()), 0, gson.toJson(cmds), true);
        logger.info("CancelShutdownCmd result : " + result);
        if (!result.contains("Success")) {
            throw new CloudRuntimeException(result);
        }

        msHost.setState(State.Up);
        msHostDao.persist(msHost);

        return readyForShutdown(cmd.getManagementServerId());
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(CancelShutdownCmd.class);
        cmdList.add(PrepareForShutdownCmd.class);
        cmdList.add(ReadyForShutdownCmd.class);
        cmdList.add(TriggerShutdownCmd.class);
        return cmdList;
    }

    private final class ShutdownTask extends TimerTask {

        private ShutdownManager shutdownManager;

        public ShutdownTask(ShutdownManager shutdownManager) {
            this.shutdownManager = shutdownManager;
        }

        @Override
        public void run() {
            try {
                Long totalPendingJobs = shutdownManager.countPendingJobs(ManagementServerNode.getManagementServerId());
                String msg = String.format("Checking for triggered shutdown... shutdownTriggered [%b] AllowAsyncJobs [%b] PendingJobCount [%d]",
                    shutdownManager.isShutdownTriggered(), shutdownManager.isPreparingForShutdown(), totalPendingJobs);
                logger.info(msg);

                // If the shutdown has been cancelled
                if (!shutdownManager.isPreparingForShutdown()) {
                    logger.info("Shutdown cancelled. Terminating the shutdown timer task");
                    this.cancel();
                    return;
                }

                // No more pending jobs. Good to terminate
                if (totalPendingJobs == 0) {
                    if (shutdownManager.isShutdownTriggered()) {
                        logger.info("Shutting down now");
                        System.exit(0);
                    }
                    if (shutdownManager.isPreparingForShutdown()) {
                        logger.info("Ready to shutdown");
                        ManagementServerHostVO msHost = msHostDao.findByMsid(ManagementServerNode.getManagementServerId());
                        msHost.setState(State.ReadyToShutDown);
                        msHostDao.persist(msHost);
                    }
                }

                logger.info("Pending jobs. Trying again later");
            } catch (final Exception e) {
                logger.error("Error trying to run shutdown task", e);
            }
        }
    }
}
