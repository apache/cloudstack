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
package com.cloud.agent;

import com.cloud.agent.api.AgentConnectStatusAnswer;
import com.cloud.agent.api.AgentConnectStatusCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupAnswer;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.properties.AgentProperties;
import com.cloud.agent.properties.AgentPropertiesFileHandler;
import com.cloud.agent.transport.Request;
import com.cloud.exception.CloudException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Status;
import com.cloud.resource.ResourceStatusUpdater;
import com.cloud.resource.ServerResource;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.nio.Link;
import org.apache.cloudstack.threadcontext.ThreadContextUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public class HostConnectProcess {
    private static final Logger logger = LogManager.getLogger(HostConnectProcess.class);

    public static final int DEFAULT_ASYNC_COMMAND_TIMEOUT_SEC =
            AgentPropertiesFileHandler.getPropertyValue(AgentProperties.ASYNC_COMMAND_TIMEOUT_SEC);

    public static final int DEFAULT_ASYNC_STARTUP_COMMAND_TIMEOUT_SEC =
            AgentPropertiesFileHandler.getPropertyValue(AgentProperties.ASYNC_STARTUP_COMMAND_TIMEOUT_SEC);

    static final long HOST_STATUS_CHECK_INITIAL_DELAY_SEC = 10;
    private long hostStatusCheckDelaySec = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.AGENT_HOST_STATUS_CHECK_DELAY_SEC);
    private final AtomicReference<ScheduledFuture<?>> hostStatusFutureRef = new AtomicReference<>();
    private final Agent agent;
    private ScheduledExecutorService hostStatusExecutor;

    public HostConnectProcess(Agent agent) {
        this.agent = agent;
        initExecutors();
    }

    private void initExecutors() {
        stop();
        var threadFactory = new NamedThreadFactory("Agent-" + HostStatusTask.class.getSimpleName());
        hostStatusExecutor = Executors.newScheduledThreadPool(1, threadFactory);
    }

    /**
     * Stops the whole connect process and cancels all scheduled asynchronous tasks.
     * Returns {@link Boolean#TRUE} if {@link HostConnectProcess} was waiting for {@link StartupAnswer}.
     */
    public boolean stop() {
        logger.debug("Stopping connect process. The process is active: {}", isInProgress());
        stopHostStatusExecutor();
        logger.debug("Stopped executor");
        Optional<? extends ScheduledFuture<?>> hostStatusOpt = Optional.ofNullable(hostStatusFutureRef.getAndSet(null))
                .filter(Predicate.not(ScheduledFuture::isCancelled));

        hostStatusOpt.ifPresent(future -> future.cancel(true));
        logger.debug("Cancelled future");

        return hostStatusOpt.isPresent();
    }

    private void stopHostStatusExecutor() {
        if (hostStatusExecutor != null) {
            hostStatusExecutor.shutdown();
            hostStatusExecutor = null;
        }
    }

    public void scheduleConnectProcess(Link link, boolean connectionTransfer) {
        logger.debug("Scheduling connect process for {}", link);
        initExecutors();

        var task = new HostStatusTask(link, connectionTransfer, agent, hostStatusFutureRef);
        var future = hostStatusExecutor.scheduleWithFixedDelay(ThreadContextUtil.wrapThreadContext(task),
                HOST_STATUS_CHECK_INITIAL_DELAY_SEC,
                hostStatusCheckDelaySec, TimeUnit.SECONDS);
        hostStatusFutureRef.set(future);
    }

    /**
     * Returns {@link Boolean#TRUE} if {@link HostStatusTask} created and scheduled.
     * That means there is already {@link Status#Connecting} process is running.
     */
    public boolean isInProgress() {
        return Optional.ofNullable(hostStatusFutureRef.get())
                .filter(Predicate.not(ScheduledFuture::isCancelled)).isPresent();
    }

    public void updateHostStatusCheckDelay(int newDelaySec) {
        logger.info("Updating host status check delay from {} to {} seconds", hostStatusCheckDelaySec, newDelaySec);
        this.hostStatusCheckDelaySec = newDelaySec;
    }

    /**
     * Task wait for the Host to be available to connect to submit {@link StartupCommand}.
     * Checks Host status on Management Server cluster and submit {@link StartupCommand} only if there is no lock and
     * Host is not {@link Status#Connecting}.
     */
    public static class HostStatusTask implements Runnable, AsyncSend {
        private final Set<Status> operationalStatuses = Set.of(Status.Connecting, Status.Up, Status.Rebalancing);

        private final Link _link;
        private final boolean _forceConnect;
        private final Agent _agent;
        private final AtomicReference<? extends ScheduledFuture<?>> _futureRef;

        public HostStatusTask(Link link, boolean forceConnect, Agent agent,
                              AtomicReference<? extends ScheduledFuture<?>> futureRef) {
            logger.debug("{} created", this.getClass().getSimpleName());
            _link = link;
            _forceConnect = forceConnect;
            _agent = agent;
            _futureRef = futureRef;
        }

        private void cancel() {
            logger.debug("Cancelling future");
            Optional.ofNullable(_futureRef.get())
                    .filter(Predicate.not(ScheduledFuture::isCancelled))
                    .ifPresent(future -> future.cancel(true));
            logger.debug("Cancelled future");
        }

        @Override
        public void run() {
            try {
                logger.debug("Running {}", getClass().getSimpleName());
                runInternal();
            } catch (Exception e) {
                logger.error("Failed to run {}", getClass().getSimpleName(), e);
            }
        }

        private void runInternal() {
            ServerAttache attache = (ServerAttache) _link.attachment();
            if (attache == null || attache.getLink() == null) {
                cancel();
                return;
            }

            AgentConnectStatusAnswer answer;
            try {
                answer = getAgentConnectStatusAnswer(attache);
            } catch (IOException e) {
                cancel();
                logger.error("The connection to {} interrupted, restarting the whole process", _link, e);
                _agent.getRequestHandler().submit(() -> _agent.reconnect(_link, null, _forceConnect));
                return;
            }
            if (answer == null) {
                logger.warn("Received empty agent connect status answer, will retry later");
                return;
            }
            Boolean lockAvailable = answer.isLockAvailable();
            Status status = answer.getHostStatus();
            if (Boolean.TRUE.equals(lockAvailable)) {
                // send startup command here
                logger.info("There is no lock and Host status is {}", status);
                try {
                    sendStartupCommand(_link, _forceConnect);
                    logger.debug("Sending startup command to {} finished", _link);
                    cancel();
                    logger.debug("Unscheduled {}", getClass().getSimpleName());
                } catch (RuntimeException e) {
                    logger.error("Failed to send startup command to {}", _link, e);
                } catch (IOException e) {
                    cancel();
                    logger.error("The connection to {} interrupted, restarting the whole process", _link, e);
                    _agent.getRequestHandler().submit(() -> _agent.reconnect(_link, null, _forceConnect));
                }
            } else {
                logger.info("There is lock and Host status is {}, will retry later", status);
            }
        }

        private AgentConnectStatusAnswer getAgentConnectStatusAnswer(ServerAttache attache) throws IOException {
            AgentConnectStatusCommand command = _agent.setupAgentConnectStatusCommand(new AgentConnectStatusCommand());
            var commands = new Command[]{command};
            try {
                return send(attache, commands, AgentConnectStatusAnswer.class, DEFAULT_ASYNC_COMMAND_TIMEOUT_SEC);
            } catch (RuntimeException e) {
                String commandName = commands[0].getClass().getSimpleName();
                logger.error("Failed to retrieve {}, will retry later", commandName, e);
                return null;
            }
        }

        public void sendStartupCommand(Link link, boolean connectionTransfer) throws IOException {
            ServerAttache attache = (ServerAttache) link.attachment();
            if (attache == null || attache.getLink() == null) {
                return;
            }
            ServerResource serverResource = _agent.getResource();
            StartupCommand[] startup = serverResource.initialize();
            if (ArrayUtils.isEmpty(startup)) {
                logger.warn("No startup commands returned from {}, Startup command sending skipped", serverResource.getName());
                return;
            }
            String logId = Optional.ofNullable(ThreadContext.get("logcontextid"))
                    .map(String.class::cast).orElse(null);
            String msHostList = _agent.getPersistentProperty("host");
            // need to downcast StartupCommand[] to Command[], otherwise logger will fail to decode JSON on MS side
            Command[] commands = new Command[startup.length];
            for (int i = 0; i < startup.length; i++) {
                StartupCommand command = startup[i];
                commands[i] = command;
                _agent.setupStartupCommand(command);
                command.setMSHostList(msHostList);
                command.setConnectionTransferred(connectionTransfer);
                if (logId != null) {
                    command.setContextParam("logid", logId);
                }
            }
            String commandName = commands[0].getClass().getSimpleName();
            boolean needAdditionalValidation = false;
            try {
                logger.debug("Sending command {} to {}", commandName, link);
                var answer = send(attache, commands, StartupAnswer.class, DEFAULT_ASYNC_STARTUP_COMMAND_TIMEOUT_SEC);
                logger.info("Received answer for {} from {}: {}, cancelling", commandName, link, answer);
            } catch (RuntimeException e) {
                Throwable cause = e.getCause();
                if (cause instanceof OperationTimedoutException) {
                    String msg = String.format("Failed to retrieve answer for the command %s, sent to %s",
                            commandName, link);
                    logger.error(msg, e);
                    needAdditionalValidation = true;
                } else {
                    String msg = String.format("Failed to send command %s to %s", commandName, link);
                    logger.error(msg, e);
                    throw new IllegalStateException(msg, e);
                }
            }
            if (needAdditionalValidation) {
                AgentConnectStatusAnswer answer = getAgentConnectStatusAnswer(attache);
                if (answer == null) {
                    logger.warn("Received empty agent connect status answer, reconnecting");
                    _agent.getRequestHandler().submit(() -> _agent.reconnect(link, null, connectionTransfer));
                    return;
                }
                Boolean lockAvailable = answer.isLockAvailable();
                Status status = answer.getHostStatus();
                if (Boolean.TRUE.equals(lockAvailable) && status != null && operationalStatuses.contains(status)) {
                    logger.info("Host is in operational state {} on {}", status, _link);
                } else if (Boolean.FALSE.equals(lockAvailable)) {
                    logger.info("Host is locked and has state {} on {}", status, _link);
                } else {
                    logger.info("Host is locked and has state {} on {}, reconnecting", status, _link);
                    _agent.getRequestHandler().submit(() -> _agent.reconnect(link, null, connectionTransfer));
                    return;
                }
            }
            if (serverResource instanceof ResourceStatusUpdater) {
                ((ResourceStatusUpdater) serverResource).registerStatusUpdater(_agent);
            }

        }

        public Agent getAgent() {
            return _agent;
        }
    }

    interface AsyncSend {
        Agent getAgent();

        default <T> T send(ServerAttache attache, Command[] commands, Class<T> answerType,
                           int asyncCommandTimeoutSec) throws IOException {
            String logId = Optional.ofNullable(ThreadContext.get("logcontextid"))
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .orElse(null);
            if (logId != null) {
                for (Command command : commands) {
                    if (command.getContextParam("logid") == null) {
                        command.setContextParam("logid", logId);
                    }
                }
            }
            Link link = attache.getLink();
            String commandName = commands[0].getClass().getSimpleName();
            Long id = Optional.ofNullable(getAgent().getId()).orElse(-1L);
            Request request = new Request(id, -1, commands, true, false);
            Answer[] answers;
            try {
                answers = attache.send(request, asyncCommandTimeoutSec);
            } catch (OperationTimedoutException e) {
                String msg = String.format("Failed to retrieve answer for command %s to %s", commandName, link);
                logger.error(msg, e);
                throw new RuntimeException(msg, e);
            } catch (CloudException e) {
                String msg = String.format("Failed to send command %s to %s", commandName, link);
                logger.error(msg, e);
                Throwable cause = e.getCause();
                if (cause instanceof ClosedChannelException) {
                    throw (ClosedChannelException) cause;
                }
                throw new RuntimeException(msg, e);
            }
            if (ArrayUtils.isEmpty(answers)) {
                String msg = String.format("Received empty %s response from %s", commandName, link);
                logger.warn(msg);
                throw new IllegalArgumentException(msg);
            }
            Answer answer = answers[0];
            String details = answer.getDetails();
            if (!answer.getResult()) {
                String msg = String.format("Received unsuccessful %s response status from %s: %s", commandName, link,
                        details);
                logger.warn(msg);
                throw new IllegalArgumentException(msg);
            }
            String answerName = answer.getClass().getSimpleName();
            if (!answerType.isInstance(answer)) {
                String msg = String.format("Received unexpected %s response type %s from %s: %s", commandName,
                        answerName, link, details);
                logger.warn(msg);
                throw new IllegalArgumentException(msg);
            }
            return answerType.cast(answer);
        }
    }
}
