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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.agent.lb.SetupMSListAnswer;
import org.apache.cloudstack.agent.lb.SetupMSListCommand;
import org.apache.cloudstack.ca.PostCertificateRenewalCommand;
import org.apache.cloudstack.ca.SetupCertificateAnswer;
import org.apache.cloudstack.ca.SetupCertificateCommand;
import org.apache.cloudstack.ca.SetupKeyStoreCommand;
import org.apache.cloudstack.ca.SetupKeystoreAnswer;
import org.apache.cloudstack.managed.context.ManagedContextTimerTask;
import org.apache.cloudstack.utils.security.KeyStoreUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CronCommand;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.PingAnswer;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.ShutdownCommand;
import com.cloud.agent.api.StartupAnswer;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.transport.Request;
import com.cloud.agent.transport.Response;
import com.cloud.exception.AgentControlChannelException;
import com.cloud.host.Host;
import com.cloud.resource.AgentStatusUpdater;
import com.cloud.resource.ResourceStatusUpdater;
import com.cloud.resource.ServerResource;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.NioConnectionException;
import com.cloud.utils.exception.TaskExecutionException;
import com.cloud.utils.nio.HandlerFactory;
import com.cloud.utils.nio.Link;
import com.cloud.utils.nio.NioClient;
import com.cloud.utils.nio.NioConnection;
import com.cloud.utils.nio.Task;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

/**
 * @config
 *         {@table
 *         || Param Name | Description | Values | Default ||
 *         || type | Type of server | Storage / Computing / Routing | No Default ||
 *         || workers | # of workers to process the requests | int | 1 ||
 *         || host | host to connect to | ip address | localhost ||
 *         || port | port to connect to | port number | 8250 ||
 *         || instance | Used to allow multiple agents running on the same host | String | none || * }
 *
 *         For more configuration options, see the individual types.
 *
 **/
public class Agent implements HandlerFactory, IAgentControl, AgentStatusUpdater {
    protected static Logger s_logger = Logger.getLogger(Agent.class);

    public enum ExitStatus {
        Normal(0), // Normal status = 0.
        Upgrade(65), // Exiting for upgrade.
        Configuration(66), // Exiting due to configuration problems.
        Error(67); // Exiting because of error.

        int value;

        ExitStatus(final int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    CopyOnWriteArrayList<IAgentControlListener> controlListeners = new CopyOnWriteArrayList<>();

    IAgentShell shell;
    NioConnection connection;
    ServerResource serverResource;
    Link link;
    Long id;

    ScheduledExecutorService selfTaskExecutor;
    ScheduledExecutorService certExecutor;
    ScheduledExecutorService hostLbCheckExecutor;

    CopyOnWriteArrayList<WatchTask> watchList = new CopyOnWriteArrayList<>();
    AtomicLong sequence = new AtomicLong(0);
    AtomicLong lastPingResponseTime = new AtomicLong(0L);
    long pingInterval = 0;
    AtomicInteger commandsInProgress = new AtomicInteger(0);

    private final AtomicReference<StartupTask> startupTask = new AtomicReference<>();
    private static final long DEFAULT_STARTUP_WAIT = 180;
    long startupWait = DEFAULT_STARTUP_WAIT;
    boolean reconnectAllowed = true;

    //For time sensitive task, e.g. PingTask
    ThreadPoolExecutor outRequestHandler;
    ExecutorService requestHandler;

    Thread shutdownThread = new ShutdownThread(this);

    private String keystoreSetupSetupPath;
    private String keystoreCertImportScriptPath;

    private String hostname;

    private String getLinkLog(final Link link) {
        if (link == null) {
            return "";
        }
        StringBuilder str = new StringBuilder();
        if (s_logger.isTraceEnabled()) {
            str.append(System.identityHashCode(link)).append("-");
        }
        str.append(link.getSocketAddress());
        return str.toString();
    }

    private String getAgentName() {
        return (serverResource != null && serverResource.isAppendAgentNameToLogs() &&
                StringUtils.isNotBlank(serverResource.getName())) ?
                serverResource.getName() :
                "Agent";
    }

    private void setupShutdownHookAndInitExecutors() {
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Adding shutdown hook");
        }
        Runtime.getRuntime().addShutdownHook(shutdownThread);
        selfTaskExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Agent-SelfTask"));
        outRequestHandler = new ThreadPoolExecutor(shell.getPingRetries(), 2 * shell.getPingRetries(), 10, TimeUnit.MINUTES,
                new SynchronousQueue<>(), new NamedThreadFactory("AgentOutRequest-Handler"));
        requestHandler = new ThreadPoolExecutor(shell.getWorkers(), 5 * shell.getWorkers(), 1, TimeUnit.DAYS,
                new LinkedBlockingQueue<>(), new NamedThreadFactory("AgentRequest-Handler"));
    }

    // for simulator use only
    public Agent(final IAgentShell shell) {
        this.shell = shell;
        link = null;
        connection = new NioClient(getAgentName(), this.shell.getNextHost(), this.shell.getPort(),
                this.shell.getWorkers(), this.shell.getSslHandshakeTimeout(), this);
        setupShutdownHookAndInitExecutors();
    }

    public Agent(final IAgentShell shell, final int localAgentId, final ServerResource resource) throws ConfigurationException {
        this.shell = shell;
        serverResource = resource;
        link = null;
        resource.setAgentControl(this);
        final String value = this.shell.getPersistentProperty(getResourceName(), "id");
        id = value != null ? Long.parseLong(value) : null;
        s_logger.info("id is " + (id != null ? id : ""));
        final Map<String, Object> params = new HashMap<>();
        // merge with properties from command line to let resource access command line parameters
        for (final Map.Entry<String, Object> cmdLineProp : this.shell.getCmdLineProperties().entrySet()) {
            params.put(cmdLineProp.getKey(), cmdLineProp.getValue());
        }
        if (!serverResource.configure(getResourceName(), params)) {
            throw new ConfigurationException("Unable to configure " + serverResource.getName());
        }
        MDC.put("agentname", getAgentName());
        final String host = this.shell.getNextHost();
        connection = new NioClient(getAgentName(), host, this.shell.getPort(), this.shell.getWorkers(),
                this.shell.getSslHandshakeTimeout(), this);
        setupShutdownHookAndInitExecutors();
        s_logger.info(String.format("Agent [id = %s, type = %s, zone = %s, pod = %s, workers = %d, host = %s, " +
                        "port = %d, local id = %d]",
                (id != null ? String.valueOf(id) : "new"), getResourceName(), this.shell.getZone(),
                this.shell.getPod(), this.shell.getWorkers(), host, this.shell.getPort(), localAgentId));
    }

    public String getVersion() {
        return shell.getVersion();
    }

    public String getResourceGuid() {
        final String guid = shell.getGuid();
        return guid + "-" + getResourceName();
    }

    public String getZone() {
        return shell.getZone();
    }

    public String getPod() {
        return shell.getPod();
    }

    protected void setLink(final Link link) {
        this.link = link;
    }

    public ServerResource getResource() {
        return serverResource;
    }

    public String getResourceName() {
        return serverResource.getClass().getSimpleName();
    }

    /**
     * In case of a software based agent restart, this method
     * can help to perform explicit garbage collection of any old
     * agent instances and its inner objects.
     */
    private void scavengeOldAgentObjects() {
        requestHandler.submit(() -> {
            try {
                Thread.sleep(2000L);
            } catch (final InterruptedException ignored) {
            } finally {
                System.gc();
            }
        });
    }

    public void start() {
        if (!serverResource.start()) {
            s_logger.error("Unable to start the resource: " + serverResource.getName());
            throw new CloudRuntimeException("Unable to start the resource: " + serverResource.getName());
        }

        keystoreSetupSetupPath = Script.findScript("scripts/util/", KeyStoreUtils.KS_SETUP_SCRIPT);
        if (keystoreSetupSetupPath == null) {
            throw new CloudRuntimeException(String.format("Unable to find the '%s' script", KeyStoreUtils.KS_SETUP_SCRIPT));
        }

        keystoreCertImportScriptPath = Script.findScript("scripts/util/", KeyStoreUtils.KS_IMPORT_SCRIPT);
        if (keystoreCertImportScriptPath == null) {
            throw new CloudRuntimeException(String.format("Unable to find the '%s' script", KeyStoreUtils.KS_IMPORT_SCRIPT));
        }

        try {
            connection.start();
        } catch (final NioConnectionException e) {
            s_logger.warn("NIO Connection Exception  " + e);
            s_logger.info("Attempted to connect to the server, but received an unexpected exception, trying again...");
        }
        while (!connection.isStartup()) {
            final String host = shell.getNextHost();
            shell.getBackoffAlgorithm().waitBeforeRetry();
            connection = new NioClient(getAgentName(), host, shell.getPort(), shell.getWorkers(),
                    shell.getSslHandshakeTimeout(), this);
            s_logger.info("Connecting to host:" + host);
            try {
                connection.start();
            } catch (final NioConnectionException e) {
                stopAndCleanupConnection(false);
                s_logger.info("Attempted to connect to the server, but received an unexpected exception, trying again...", e);
            }
        }
        shell.updateConnectedHost();
        scavengeOldAgentObjects();

    }

    public void stop(final String reason, final String detail) {
        s_logger.info("Stopping the agent: Reason = " + reason + (detail != null ? ": Detail = " + detail : ""));
        reconnectAllowed = false;
        if (connection != null) {
            final ShutdownCommand cmd = new ShutdownCommand(reason, detail);
            try {
                if (link != null) {
                    final Request req = new Request(id != null ? id : -1, -1, cmd, false);
                    link.send(req.toBytes());
                }
            } catch (final ClosedChannelException e) {
                s_logger.warn("Unable to send: " + cmd.toString());
            } catch (final Exception e) {
                s_logger.warn("Unable to send: " + cmd.toString() + " due to exception: ", e);
            }
            s_logger.debug("Sending shutdown to management server");
            try {
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
                s_logger.debug("Who the heck interrupted me here?");
            }
            connection.stop();
            connection = null;
            link = null;
        }

        if (serverResource != null) {
            serverResource.stop();
            serverResource = null;
        }

        if (startupTask.get() != null) {
            startupTask.set(null);
        }

        if (outRequestHandler != null) {
            outRequestHandler.shutdownNow();
            outRequestHandler = null;
        }

        if (requestHandler != null) {
            requestHandler.shutdown();
            requestHandler = null;
        }

        if (selfTaskExecutor != null) {
            selfTaskExecutor.shutdown();
            selfTaskExecutor = null;
        }

        if (hostLbCheckExecutor != null) {
            hostLbCheckExecutor.shutdown();
            hostLbCheckExecutor = null;
        }

        if (certExecutor != null) {
            certExecutor.shutdown();
            certExecutor = null;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        s_logger.info("Set agent id " + id);
        this.id = id;
        shell.setPersistentProperty(getResourceName(), "id", Long.toString(id));
    }

    private void scheduleCertificateRenewalTask() {
        String name = "CertificateRenewalTask";
        if (certExecutor != null && !certExecutor.isShutdown()) {
            certExecutor.shutdown();
            try {
                if (!certExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    certExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(
                            String.format("Forcing %s shutdown as it did not shutdown in the desired time due to: %s",
                                    name, e.getMessage()));
                }
                certExecutor.shutdownNow();
            }
        }
        certExecutor = Executors.newSingleThreadScheduledExecutor((new NamedThreadFactory(name)));
        certExecutor.schedule(new PostCertificateRenewalTask(this), 5, TimeUnit.SECONDS);
    }

    private synchronized void scheduleHostLBCheckerTask(final long checkInterval) {
        String name = "HostLBCheckerTask";
        if (hostLbCheckExecutor != null && !hostLbCheckExecutor.isShutdown()) {
            hostLbCheckExecutor.shutdown();
            try {
                if (!hostLbCheckExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    hostLbCheckExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(
                            String.format("Forcing %s shutdown as it did not shutdown in the desired time due to: %s",
                                    name, e.getMessage()));
                }
                hostLbCheckExecutor.shutdownNow();
            }
        }
        if (checkInterval > 0L) {
            hostLbCheckExecutor = Executors.newSingleThreadScheduledExecutor((new NamedThreadFactory(name)));
            hostLbCheckExecutor.scheduleAtFixedRate(new PreferredHostCheckerTask(), checkInterval, checkInterval,
                    TimeUnit.MILLISECONDS);
        }
    }

    public void scheduleWatch(final Link link, final Request request, final long delay, final long period) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Adding a watch list");
        }
        final WatchTask task = new WatchTask(link, request, this);
        selfTaskExecutor.scheduleAtFixedRate(task, delay, period, TimeUnit.MILLISECONDS);
        watchList.add(task);
    }

    public void triggerUpdate() {
        PingCommand command = serverResource.getCurrentStatus(getId());
        command.setOutOfBand(true);
        s_logger.debug("Sending out of band ping");
        final Request request = new Request(id, -1, command, false);
        request.setSequence(getNextSequence());
        try {
            link.send(request.toBytes());
        } catch (final ClosedChannelException e) {
            s_logger.warn("Unable to send ping update: " + request.toString());
        }
    }

    protected void cancelTasks() {
        for (final WatchTask task : watchList) {
            task.cancel();
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Clearing watch list: " + watchList.size());
        }
        watchList.clear();
    }

    /**
     * Cleanup agent zone properties.
     *
     * Unset zone, cluster and pod values so that host is not added back
     * when service is restarted. This will be set to proper values
     * when host is added back
     */
    protected void cleanupAgentZoneProperties() {
        shell.setPersistentProperty(null, "zone", "");
        shell.setPersistentProperty(null, "cluster", "");
        shell.setPersistentProperty(null, "pod", "");
    }

    public void lockStartupTask(final Link link) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug(String.format("Creating startup task - %s", getLinkLog(link)));
        }
        StartupTask currentTask = startupTask.get();
        if (currentTask != null) {
            s_logger.warn("A Startup task is already locked or in progress.");
            return;
        }
        currentTask = new StartupTask(link);
        if (startupTask.compareAndSet(null, currentTask)) {
            selfTaskExecutor.schedule(currentTask, startupWait, TimeUnit.SECONDS);
            return;
        }
        s_logger.warn("Failed to lock a StartupTask");
    }

    protected boolean cancelStartupTask() {
        StartupTask task = startupTask.getAndSet(null);
        if (task != null) {
            task.cancel();
            return true;
        }
        return false;
    }

    public void sendStartup(final Link link) {
        final StartupCommand[] startup = serverResource.initialize();
        if (startup != null) {
            final String msHostList = shell.getPersistentProperty(null, "host");
            final Command[] commands = new Command[startup.length];
            for (int i = 0; i < startup.length; i++) {
                setupStartupCommand(startup[i]);
                startup[i].setMSHostList(msHostList);
                commands[i] = startup[i];
            }
            final Request request = new Request(id != null ? id : -1, -1, commands, false, false);
            request.setSequence(getNextSequence());

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Sending Startup: " + request.toString());
            }
            lockStartupTask(link);
            try {
                link.send(request.toBytes());
            } catch (final ClosedChannelException e) {
                s_logger.warn(String.format("Unable to send request to %s due to '%s', request: %s",
                        getLinkLog(link), e.getMessage(), request));
            }

            if (serverResource instanceof ResourceStatusUpdater) {
                ((ResourceStatusUpdater) serverResource).registerStatusUpdater(this);
            }
        }
    }

    protected String retrieveHostname() {
        if (s_logger.isTraceEnabled()) {
            s_logger.trace(" Retrieving hostname " + serverResource.getClass().getSimpleName());
        }
        final Script command = new Script("hostname", 500, s_logger);
        final OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
        final String result = command.execute(parser);
        if (result != null) {
            return parser.getLine();
        }
        try {
            InetAddress addr = InetAddress.getLocalHost();
            return addr.toString();
        } catch (final UnknownHostException e) {
            s_logger.warn("unknown host? ", e);
            throw new CloudRuntimeException("Cannot get local IP address");
        }
    }

    protected void setupStartupCommand(final StartupCommand startup) {
        startup.setId(getId());
        if (StringUtils.isBlank(startup.getName())) {
            if (StringUtils.isBlank(hostname)) {
                hostname = retrieveHostname();
            }
            startup.setName(hostname);
        }
        startup.setDataCenter(getZone());
        startup.setPod(getPod());
        startup.setGuid(getResourceGuid());
        startup.setResourceName(getResourceName());
        startup.setVersion(getVersion());
    }

    @Override
    public Task create(final Task.Type type, final Link link, final byte[] data) {
        return new ServerHandler(type, link, data);
    }

    protected void closeAndTerminateLink(final Link link) {
        if (link == null) {
            return;
        }
        link.close();
        link.terminated();
    }

    protected void stopAndCleanupConnection(boolean waitForStop) {
        if (connection == null) {
            return;
        }
        connection.stop();
        try {
            connection.cleanUp();
        } catch (final IOException e) {
            s_logger.warn("Fail to clean up old connection. " + e);
        }
        if (!waitForStop) {
            return;
        }
        do {
            shell.getBackoffAlgorithm().waitBeforeRetry();
        } while (connection.isStartup());
    }

    protected void reconnect(final Link link) {
        if (!reconnectAllowed) {
            s_logger.debug(String.format("Reconnect requested but it is not allowed %s",
                    getLinkLog(link)));
            return;
        }
        cancelStartupTask();
        closeAndTerminateLink(link);
        closeAndTerminateLink(this.link);
        setLink(null);
        cancelTasks();
        serverResource.disconnected();
        s_logger.info("Lost connection to host: " + shell.getConnectedHost() + ". Attempting reconnection while we still have " + commandsInProgress.get() + " commands in progress.");
        stopAndCleanupConnection(true);
        do {
            final String host = shell.getNextHost();
            connection = new NioClient(getAgentName(), host, shell.getPort(), shell.getWorkers(), shell.getSslHandshakeTimeout(), this);
            s_logger.info(String.format("Reconnecting to host: %s", host));
            try {
                connection.start();
            } catch (final NioConnectionException e) {
                s_logger.info("Attempted to re-connect to the server, but received an unexpected exception, trying again...", e);
                stopAndCleanupConnection(false);
            }
            shell.getBackoffAlgorithm().waitBeforeRetry();
        } while (!connection.isStartup());
        shell.updateConnectedHost();
        s_logger.info("Connected to the host: " + shell.getConnectedHost());
    }

    public void processStartupAnswer(final Answer answer, final Response response, final Link link) {
        boolean answerValid = cancelStartupTask();
        final StartupAnswer startup = (StartupAnswer)answer;
        if (!startup.getResult()) {
            s_logger.error("Not allowed to connect to the server: " + answer.getDetails());
            if (serverResource != null && !serverResource.isExitOnFailures()) {
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace(String.format("%s does not allow exit on failure, reconnecting",
                            serverResource.getClass().getSimpleName()));
                }
                reconnect(link);
                return;
            }
            System.exit(1);
        }
        if (!answerValid) {
            s_logger.warn("Threw away a startup answer because we're reconnecting.");
            return;
        }

        s_logger.info("Process agent startup answer, agent id = " + startup.getHostId());

        setId(startup.getHostId());
        pingInterval = startup.getPingInterval() * 1000L; // change to ms.

        updateLastPingResponseTime();
        scheduleWatch(link, response, pingInterval, pingInterval);

        outRequestHandler.setKeepAliveTime(2 * pingInterval, TimeUnit.MILLISECONDS);

        s_logger.info("Startup Response Received: agent id = " + getId());
    }

    protected void processRequest(final Request request, final Link link) {
        boolean requestLogged = false;
        Response response = null;
        try {
            final Command[] cmds = request.getCommands();
            final Answer[] answers = new Answer[cmds.length];

            for (int i = 0; i < cmds.length; i++) {
                final Command cmd = cmds[i];
                Answer answer;
                try {
                    if (cmd.getContextParam("logid") != null) {
                        MDC.put("logcontextid", cmd.getContextParam("logid"));
                    }
                    if (s_logger.isDebugEnabled()) {
                        if (!requestLogged) // ensures request is logged only once per method call
                        {
                            final String requestMsg = request.toString();
                            if (requestMsg != null) {
                                s_logger.debug("Request:" + requestMsg);
                            }
                            requestLogged = true;
                        }
                        s_logger.debug("Processing command: " + cmd.toString());
                    }

                    if (cmd instanceof CronCommand) {
                        final CronCommand watch = (CronCommand)cmd;
                        scheduleWatch(link, request, watch.getInterval() * 1000L, watch.getInterval() * 1000L);
                        answer = new Answer(cmd, true, null);
                    } else if (cmd instanceof ShutdownCommand) {
                        final ShutdownCommand shutdown = (ShutdownCommand)cmd;
                        s_logger.debug("Received shutdownCommand, due to: " + shutdown.getReason());
                        cancelTasks();
                        if (shutdown.isRemoveHost()) {
                            cleanupAgentZoneProperties();
                        }
                        reconnectAllowed = false;
                        answer = new Answer(cmd, true, null);
                    } else if (cmd instanceof ReadyCommand && ((ReadyCommand)cmd).getDetails() != null) {
                        s_logger.debug("Not ready to connect to mgt server: " + ((ReadyCommand)cmd).getDetails());
                        if (serverResource != null && !serverResource.isExitOnFailures()) {
                            if (s_logger.isTraceEnabled()) {
                                s_logger.trace(String.format("%s does not allow exit on failure, reconnecting",
                                        serverResource.getClass().getSimpleName()));
                            }
                            reconnect(link);
                            return;
                        }
                        System.exit(1);
                        return;
                    } else if (cmd instanceof MaintainCommand) {
                        s_logger.debug("Received maintainCommand, do not cancel current tasks");
                        answer = new MaintainAnswer((MaintainCommand)cmd);
                    } else if (cmd instanceof AgentControlCommand) {
                        answer = null;
                        for (final IAgentControlListener listener : controlListeners) {
                            answer = listener.processControlRequest(request, (AgentControlCommand)cmd);
                            if (answer != null) {
                                break;
                            }
                        }

                        if (answer == null) {
                            s_logger.warn("No handler found to process cmd: " + cmd.toString());
                            answer = new AgentControlAnswer(cmd);
                        }
                    } else if (cmd instanceof SetupKeyStoreCommand && ((SetupKeyStoreCommand) cmd).isHandleByAgent()) {
                        answer = setupAgentKeystore((SetupKeyStoreCommand) cmd);
                    } else if (cmd instanceof SetupCertificateCommand && ((SetupCertificateCommand) cmd).isHandleByAgent()) {
                        answer = setupAgentCertificate((SetupCertificateCommand) cmd);
                        if (Host.Type.Routing.equals(serverResource.getType())) {
                            scheduleCertificateRenewalTask();
                        }
                    } else if (cmd instanceof SetupMSListCommand) {
                        answer = setupManagementServerList((SetupMSListCommand) cmd);
                    } else {
                        if (cmd instanceof ReadyCommand) {
                            processReadyCommand(cmd);
                        }
                        commandsInProgress.incrementAndGet();
                        try {
                            answer = serverResource.executeRequest(cmd);
                        } finally {
                            commandsInProgress.decrementAndGet();
                        }
                        if (answer == null) {
                            s_logger.debug("Response: unsupported command" + cmd.toString());
                            answer = Answer.createUnsupportedCommandAnswer(cmd);
                        }
                    }
                } catch (final Throwable th) {
                    s_logger.warn("Caught: ", th);
                    final StringWriter writer = new StringWriter();
                    th.printStackTrace(new PrintWriter(writer));
                    answer = new Answer(cmd, false, writer.toString());
                }

                answers[i] = answer;
                if (!answer.getResult() && request.stopOnError()) {
                    for (i++; i < cmds.length; i++) {
                        answers[i] = new Answer(cmds[i], false, "Stopped by previous failure");
                    }
                    break;
                }
            }
            response = new Response(request, answers);
        } finally {
            if (s_logger.isDebugEnabled()) {
                final String responseMsg = response.toString();
                if (responseMsg != null) {
                    s_logger.debug(response.toString());
                }
            }

            if (response != null) {
                try {
                    link.send(response.toBytes());
                } catch (final ClosedChannelException e) {
                    s_logger.warn("Unable to send response: " + response.toString());
                }
            }
        }
    }

    public Answer setupAgentKeystore(final SetupKeyStoreCommand cmd) {
        final String keyStorePassword = cmd.getKeystorePassword();
        final long validityDays = cmd.getValidityDays();

        s_logger.debug("Setting up agent keystore file and generating CSR");

        final File agentFile = PropertiesUtil.findConfigFile("agent.properties");
        if (agentFile == null) {
            return new Answer(cmd, false, "Failed to find agent.properties file");
        }
        final String keyStoreFile = agentFile.getParent() + "/" + KeyStoreUtils.KS_FILENAME;
        final String csrFile = agentFile.getParent() + "/" + KeyStoreUtils.CSR_FILENAME;

        String storedPassword = shell.getPersistentProperty(null, KeyStoreUtils.KS_PASSPHRASE_PROPERTY);
        if (StringUtils.isEmpty(storedPassword)) {
            storedPassword = keyStorePassword;
            shell.setPersistentProperty(null, KeyStoreUtils.KS_PASSPHRASE_PROPERTY, storedPassword);
        }

        Script script = new Script(keystoreSetupSetupPath, 300000, s_logger);
        script.add(agentFile.getAbsolutePath());
        script.add(keyStoreFile);
        script.add(storedPassword);
        script.add(String.valueOf(validityDays));
        script.add(csrFile);
        String result = script.execute();
        if (result != null) {
            throw new CloudRuntimeException("Unable to setup keystore file");
        }

        final String csrString;
        try {
            csrString = FileUtils.readFileToString(new File(csrFile), Charset.defaultCharset());
        } catch (IOException e) {
            throw new CloudRuntimeException("Unable to read generated CSR file", e);
        }
        return new SetupKeystoreAnswer(csrString);
    }

    private Answer setupAgentCertificate(final SetupCertificateCommand cmd) {
        final String certificate = cmd.getCertificate();
        final String privateKey = cmd.getPrivateKey();
        final String caCertificates = cmd.getCaCertificates();

        s_logger.debug("Importing received certificate to agent's keystore");

        final File agentFile = PropertiesUtil.findConfigFile("agent.properties");
        if (agentFile == null) {
            return new Answer(cmd, false, "Failed to find agent.properties file");
        }
        final String keyStoreFile = agentFile.getParent() + "/" + KeyStoreUtils.KS_FILENAME;
        final String certFile = agentFile.getParent() + "/" + KeyStoreUtils.CERT_FILENAME;
        final String privateKeyFile = agentFile.getParent() + "/" + KeyStoreUtils.PKEY_FILENAME;
        final String caCertFile = agentFile.getParent() + "/" + KeyStoreUtils.CACERT_FILENAME;

        try {
            FileUtils.writeStringToFile(new File(certFile), certificate, Charset.defaultCharset());
            FileUtils.writeStringToFile(new File(caCertFile), caCertificates, Charset.defaultCharset());
            s_logger.debug("Saved received client certificate to: " + certFile);
        } catch (IOException e) {
            throw new CloudRuntimeException("Unable to save received agent client and ca certificates", e);
        }

        String ksPassphrase = shell.getPersistentProperty(null, KeyStoreUtils.KS_PASSPHRASE_PROPERTY);
        Script script = new Script(keystoreCertImportScriptPath, 300000, s_logger);
        script.add(agentFile.getAbsolutePath());
        script.add(ksPassphrase);
        script.add(keyStoreFile);
        script.add(KeyStoreUtils.AGENT_MODE);
        script.add(certFile);
        script.add("");
        script.add(caCertFile);
        script.add("");
        script.add(privateKeyFile);
        script.add(privateKey);
        String result = script.execute();
        if (result != null) {
            throw new CloudRuntimeException("Unable to import certificate into keystore file");
        }
        return new SetupCertificateAnswer(true);
    }

    private void processManagementServerList(final List<String> msList, final String lbAlgorithm, final Long lbCheckInterval) {
        if (CollectionUtils.isNotEmpty(msList) && StringUtils.isNotEmpty(lbAlgorithm)) {
            try {
                final String newMSHosts = String.format("%s%s%s", com.cloud.utils.StringUtils.toCSVList(msList), IAgentShell.hostLbAlgorithmSeparator, lbAlgorithm);
                shell.setPersistentProperty(null, "host", newMSHosts);
                shell.setHosts(newMSHosts);
                shell.resetHostCounter();
                s_logger.info("Processed new management server list: " + newMSHosts);
            } catch (final Exception e) {
                throw new CloudRuntimeException("Could not persist received management servers list", e);
            }
        }
        if ("shuffle".equals(lbAlgorithm)) {
            scheduleHostLBCheckerTask(0);
        } else {
            scheduleHostLBCheckerTask(shell.getLbCheckerInterval(lbCheckInterval));
        }
    }

    private Answer setupManagementServerList(final SetupMSListCommand cmd) {
        processManagementServerList(cmd.getMsList(), cmd.getLbAlgorithm(), cmd.getLbCheckInterval());
        return new SetupMSListAnswer(true);
    }

    public void processResponse(final Response response, final Link link) {
        final Answer answer = response.getAnswer();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Received response: " + response.toString());
        }
        if (answer instanceof StartupAnswer) {
            processStartupAnswer(answer, response, link);
        } else if (answer instanceof AgentControlAnswer) {
            // Notice, we are doing callback while holding a lock!
            for (final IAgentControlListener listener : controlListeners) {
                listener.processControlResponse(response, (AgentControlAnswer)answer);
            }
        } else if (answer instanceof PingAnswer && (((PingAnswer) answer).isSendStartup()) && reconnectAllowed) {
            s_logger.info("Management server requested startup command to reinitialize the agent");
            sendStartup(link);
        } else {
            updateLastPingResponseTime();
        }
    }

    public void processReadyCommand(final Command cmd) {
        final ReadyCommand ready = (ReadyCommand)cmd;
        // Set human readable sizes;
        Boolean humanReadable = ready.getEnableHumanReadableSizes();
        if (humanReadable != null){
            NumbersUtil.enableHumanReadableSizes = humanReadable;
        }

        s_logger.info("Processing agent ready command, agent id = " + ready.getHostId());
        if (ready.getHostId() != null) {
            setId(ready.getHostId());
        }

        processManagementServerList(ready.getMsHostList(), ready.getLbAlgorithm(), ready.getLbCheckInterval());

        s_logger.info("Ready command is processed for agent id = " + getId());
    }

    public void processOtherTask(final Task task) {
        final Object obj = task.get();
        if (obj instanceof Response) {
            if (System.currentTimeMillis() - lastPingResponseTime.get() > pingInterval * shell.getPingRetries()) {
                s_logger.error("Ping Interval has gone past " + pingInterval * shell.getPingRetries() + ". Won't reconnect to mgt server, as connection is still alive");
                return;
            }

            final PingCommand ping = serverResource.getCurrentStatus(getId());
            final Request request = new Request(id, -1, ping, false);
            request.setSequence(getNextSequence());
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Sending ping: " + request.toString());
            }

            try {
                task.getLink().send(request.toBytes());
                //if i can send pingcommand out, means the link is ok
                updateLastPingResponseTime();
            } catch (final ClosedChannelException e) {
                s_logger.warn(String.format("Unable to send request to %s due to '%s', request: %s",
                        getLinkLog(task.getLink()), e.getMessage(), request));
            }

        } else if (obj instanceof Request) {
            final Request req = (Request)obj;
            final Command command = req.getCommand();
            if (command.getContextParam("logid") != null) {
                MDC.put("logcontextid", command.getContextParam("logid"));
            }
            Answer answer = null;
            commandsInProgress.incrementAndGet();
            try {
                answer = serverResource.executeRequest(command);
            } finally {
                commandsInProgress.decrementAndGet();
            }
            if (answer != null) {
                final Response response = new Response(req, answer);

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Watch Sent: " + response.toString());
                }
                try {
                    task.getLink().send(response.toBytes());
                } catch (final ClosedChannelException e) {
                    s_logger.warn("Unable to send response: " + response.toString());
                }
            }
        } else {
            s_logger.warn("Ignoring an unknown task");
        }
    }

    public void updateLastPingResponseTime() {
        lastPingResponseTime.set(System.currentTimeMillis());
    }

    protected long getNextSequence() {
        return sequence.getAndIncrement();
    }

    @Override
    public void registerControlListener(final IAgentControlListener listener) {
        controlListeners.add(listener);
    }

    @Override
    public void unregisterControlListener(final IAgentControlListener listener) {
        controlListeners.remove(listener);
    }

    @Override
    public AgentControlAnswer sendRequest(final AgentControlCommand cmd, final int timeoutInMilliseconds) throws AgentControlChannelException {
        final Request request = new Request(getId(), -1, new Command[] {cmd}, true, false);
        request.setSequence(getNextSequence());
        final AgentControlListener listener = new AgentControlListener(request);
        registerControlListener(listener);
        try {
            postRequest(request);
            try {
                listener.wait(timeoutInMilliseconds);
            } catch (final InterruptedException e) {
                s_logger.warn("sendRequest is interrupted, exit waiting");
            }
            return listener.getAnswer();
        } finally {
            unregisterControlListener(listener);
        }
    }

    @Override
    public void postRequest(final AgentControlCommand cmd) throws AgentControlChannelException {
        final Request request = new Request(getId(), -1, new Command[] {cmd}, true, false);
        request.setSequence(getNextSequence());
        postRequest(request);
    }

    private void postRequest(final Request request) throws AgentControlChannelException {
        if (link != null) {
            try {
                link.send(request.toBytes());
            } catch (final ClosedChannelException e) {
                s_logger.warn("Unable to post agent control request: " + request.toString());
                throw new AgentControlChannelException("Unable to post agent control request due to " + e.getMessage());
            }
        } else {
            throw new AgentControlChannelException("Unable to post agent control request as link is not available");
        }
    }

    public class AgentControlListener implements IAgentControlListener {
        private AgentControlAnswer _answer;
        private final Request _request;

        public AgentControlListener(final Request request) {
            _request = request;
        }

        public AgentControlAnswer getAnswer() {
            return _answer;
        }

        @Override
        public Answer processControlRequest(final Request request, final AgentControlCommand cmd) {
            return null;
        }

        @Override
        public void processControlResponse(final Response response, final AgentControlAnswer answer) {
            if (_request.getSequence() == response.getSequence()) {
                _answer = answer;
                synchronized (this) {
                    notifyAll();
                }
            }
        }
    }

    protected class ShutdownThread extends Thread {
        Agent _agent;

        public ShutdownThread(final Agent agent) {
            super("AgentShutdownThread");
            _agent = agent;
        }

        @Override
        public void run() {
            _agent.stop(ShutdownCommand.Requested, null);
        }
    }

    public class WatchTask extends ManagedContextTimerTask {
        protected Request _request;
        protected Agent _agent;
        protected Link _link;

        public WatchTask(final Link link, final Request request, final Agent agent) {
            super();
            _request = request;
            _link = link;
            _agent = agent;
        }

        @Override
        protected void runInContext() {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Scheduling " + (_request instanceof Response ? "Ping" : "Watch Task"));
            }
            try {
                if (_request instanceof Response) {
                    outRequestHandler.submit(new ServerHandler(Task.Type.OTHER, _link, _request));
                } else {
                    _link.schedule(new ServerHandler(Task.Type.OTHER, _link, _request));
                }
            } catch (final ClosedChannelException e) {
                s_logger.warn("Unable to schedule task because channel is closed");
            }
        }
    }

    public class StartupTask extends ManagedContextTimerTask {
        protected Link _link;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        public StartupTask(final Link link) {
            s_logger.debug("Startup task created");
            _link = link;
        }

        @Override
        public boolean cancel() {
            // TimerTask.cancel may fail depends on the calling context
            if (cancelled.compareAndSet(false, true)) {
                startupWait = DEFAULT_STARTUP_WAIT;
                s_logger.debug("Startup task cancelled");
                return super.cancel();
            }
            return true;
        }

        @Override
        protected void runInContext() {
            if (cancelled.compareAndSet(false, true)) {
                s_logger.info("The running startup command is now invalid. Attempting reconnect");
                startupTask.set(null);
                startupWait = DEFAULT_STARTUP_WAIT * 2;
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(String.format("Executing reconnect from task - %s", getLinkLog(_link)));
                }
                reconnect(_link);
            }
        }
    }

    public class AgentRequestHandler extends Task {
        public AgentRequestHandler(final Task.Type type, final Link link, final Request req) {
            super(type, link, req);
        }

        @Override
        protected void doTask(final Task task) throws TaskExecutionException {
            final Request req = (Request)get();
            if (!(req instanceof Response)) {
                processRequest(req, task.getLink());
            }
        }
    }

    public class ServerHandler extends Task {
        public ServerHandler(final Task.Type type, final Link link, final byte[] data) {
            super(type, link, data);
        }

        public ServerHandler(final Task.Type type, final Link link, final Request req) {
            super(type, link, req);
        }

        @Override
        public void doTask(final Task task) throws TaskExecutionException {
            if (task.getType() == Task.Type.CONNECT) {
                shell.getBackoffAlgorithm().reset();
                setLink(task.getLink());
                sendStartup(task.getLink());
            } else if (task.getType() == Task.Type.DATA) {
                Request request;
                try {
                    request = Request.parse(task.getData());
                    if (request instanceof Response) {
                        //It's for pinganswer etc, should be processed immediately.
                        processResponse((Response)request, task.getLink());
                    } else {
                        //put the requests from mgt server into another thread pool, as the request may take a longer time to finish. Don't block the NIO main thread pool
                        //processRequest(request, task.getLink());
                        requestHandler.submit(new AgentRequestHandler(getType(), getLink(), request));
                    }
                } catch (final ClassNotFoundException e) {
                    s_logger.error("Unable to find this request ");
                } catch (final Exception e) {
                    s_logger.error("Error parsing task", e);
                }
            } else if (task.getType() == Task.Type.DISCONNECT) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(String.format("Executing disconnect task - %s", getLinkLog(task.getLink())));
                }
                reconnect(task.getLink());
            } else if (task.getType() == Task.Type.OTHER) {
                processOtherTask(task);
            }
        }
    }

    /**
     * Task stops the current agent and launches a new agent
     * when there are no outstanding jobs in the agent's task queue
     */
    public class PostCertificateRenewalTask extends ManagedContextTimerTask {

        private Agent agent;

        public PostCertificateRenewalTask(final Agent agent) {
            this.agent = agent;
        }

        @Override
        protected void runInContext() {
            while (true) {
                try {
                    if (commandsInProgress.get() == 0) {
                        s_logger.debug("Running post certificate renewal task to restart services.");

                        // Let the resource perform any post certificate renewal cleanups
                        serverResource.executeRequest(new PostCertificateRenewalCommand());

                        IAgentShell shell = agent.shell;
                        ServerResource resource = agent.serverResource.getClass().getDeclaredConstructor().newInstance();

                        // Stop current agent
                        agent.cancelTasks();
                        agent.reconnectAllowed = false;
                        Runtime.getRuntime().removeShutdownHook(agent.shutdownThread);
                        agent.stop(ShutdownCommand.Requested, "Restarting due to new X509 certificates");

                        // Nullify references for GC
                        agent.watchList = null;
                        agent.shutdownThread = null;
                        agent.controlListeners = null;
                        agent = null;

                        // Start a new agent instance
                        shell.launchNewAgent(resource);
                        return;
                    }
                    if (s_logger.isTraceEnabled()) {
                        s_logger.trace("Other tasks are in progress, will retry post certificate renewal command after few seconds");
                    }
                    Thread.sleep(5000);
                } catch (final Exception e) {
                    s_logger.warn("Failed to execute post certificate renewal command:", e);
                    break;
                }
            }
        }
    }

    public class PreferredHostCheckerTask extends ManagedContextTimerTask {

        @Override
        protected void runInContext() {
            try {
                final String[] msList = shell.getHosts();
                if (msList == null || msList.length < 1) {
                    return;
                }
                final String preferredHost  = msList[0];
                final String connectedHost = shell.getConnectedHost();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Running preferred host checker task, connected host=" + connectedHost + ", preferred host=" + preferredHost);
                }
                if (preferredHost == null || preferredHost.equals(connectedHost) || link == null) {
                    return;
                }
                boolean isHostUp = false;
                try (final Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(preferredHost, shell.getPort()), 5000);
                    isHostUp = true;
                } catch (final IOException e) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Host: " + preferredHost + " is not reachable");
                    }
                }
                if (isHostUp && link != null && commandsInProgress.get() == 0) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Preferred host " + preferredHost + " is found to be reachable, trying to reconnect");
                    }
                    shell.resetHostCounter();
                    reconnect(link);
                }
            } catch (Throwable t) {
                s_logger.error("Error caught while attempting to connect to preferred host", t);
            }
        }
    }
}
