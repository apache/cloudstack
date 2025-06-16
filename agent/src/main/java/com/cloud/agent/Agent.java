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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CronCommand;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.MigrateAgentConnectionAnswer;
import com.cloud.agent.api.MigrateAgentConnectionCommand;
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
import com.cloud.utils.StringUtils;
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
    protected Logger logger = LogManager.getLogger(getClass());

    public enum ExitStatus {
        Normal(0), // Normal status = 0.
        Upgrade(65), // Exiting for upgrade.
        Configuration(66), // Exiting due to configuration problems.
        Error(67); // Exiting because of error.

        final int value;

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
    String _uuid;
    String _name;

    ScheduledExecutorService selfTaskExecutor;
    ScheduledExecutorService certExecutor;
    ScheduledExecutorService hostLbCheckExecutor;

    CopyOnWriteArrayList<ScheduledFuture<?>> watchList = new CopyOnWriteArrayList<>();
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

    protected String getLinkLog(final Link link) {
        if (link == null) {
            return "";
        }
        StringBuilder str = new StringBuilder();
        if (logger.isTraceEnabled()) {
            str.append(System.identityHashCode(link)).append("-");
        }
        str.append(link.getSocketAddress());
        return str.toString();
    }

    protected String getAgentName() {
        return (serverResource != null && serverResource.isAppendAgentNameToLogs() &&
                StringUtils.isNotBlank(serverResource.getName())) ?
                serverResource.getName() :
                "Agent";
    }

    protected void setupShutdownHookAndInitExecutors() {
        logger.trace("Adding shutdown hook");
        Runtime.getRuntime().addShutdownHook(shutdownThread);
        selfTaskExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Agent-SelfTask"));
        outRequestHandler = new ThreadPoolExecutor(shell.getPingRetries(), 2 * shell.getPingRetries(), 10, TimeUnit.MINUTES,
                new SynchronousQueue<>(), new NamedThreadFactory("AgentOutRequest-Handler"));
        requestHandler = new ThreadPoolExecutor(shell.getWorkers(), 5 * shell.getWorkers(), 1, TimeUnit.DAYS,
                new LinkedBlockingQueue<>(), new NamedThreadFactory("AgentRequest-Handler"));
    }

    /**
     * Constructor for the {@code Agent} class, intended for simulator use only.
     *
     * <p>This constructor initializes the agent with a provided {@link IAgentShell}.
     * It sets up the necessary NIO client connection, establishes a shutdown hook,
     * and initializes the thread executors.
     *
     * @param shell the {@link IAgentShell} instance that provides agent configuration and runtime information.
     */
    public Agent(final IAgentShell shell) {
        this.shell = shell;
        this.link = null;
        this.connection = new NioClient(
                getAgentName(),
                this.shell.getNextHost(),
                this.shell.getPort(),
                this.shell.getWorkers(),
                this.shell.getSslHandshakeTimeout(),
                this
        );
        setupShutdownHookAndInitExecutors();
    }

    public Agent(final IAgentShell shell, final int localAgentId, final ServerResource resource) throws ConfigurationException {
        this.shell = shell;
        serverResource = resource;
        link = null;
        resource.setAgentControl(this);
        final String value = shell.getPersistentProperty(getResourceName(), "id");
        _uuid = shell.getPersistentProperty(getResourceName(), "uuid");
        _name = shell.getPersistentProperty(getResourceName(), "name");
        id = value != null ? Long.parseLong(value) : null;
        logger.info("Initialising agent [id: {}, uuid: {}, name: {}]", ObjectUtils.defaultIfNull(id, ""), _uuid, _name);

        final Map<String, Object> params = new HashMap<>();
        // merge with properties from command line to let resource access command line parameters
        for (final Map.Entry<String, Object> cmdLineProp : this.shell.getCmdLineProperties().entrySet()) {
            params.put(cmdLineProp.getKey(), cmdLineProp.getValue());
        }
        if (!serverResource.configure(getResourceName(), params)) {
            throw new ConfigurationException("Unable to configure " + serverResource.getName());
        }
        ThreadContext.put("agentname", getAgentName());
        final String host = this.shell.getNextHost();
        connection = new NioClient(getAgentName(), host, this.shell.getPort(), this.shell.getWorkers(),
                this.shell.getSslHandshakeTimeout(), this);
        setupShutdownHookAndInitExecutors();
        logger.info("{} with host = {}, local id = {}", this, host, localAgentId);
    }


    @Override
    public String toString() {
        return String.format("Agent [id = %s, uuid = %s, name = %s, type = %s, zone = %s, pod = %s, workers = %d, port = %d]",
                ObjectUtils.defaultIfNull(id, "new"),
                _uuid,
                _name,
                getResourceName(),
                this.shell.getZone(),
                this.shell.getPod(),
                this.shell.getWorkers(),
                this.shell.getPort());
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
            String msg = String.format("Unable to start the resource: %s", serverResource.getName());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
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
            logger.warn("Attempt to connect to server generated NIO Connection Exception {}, trying again", e.getLocalizedMessage());
        }
        while (!connection.isStartup()) {
            final String host = shell.getNextHost();
            shell.getBackoffAlgorithm().waitBeforeRetry();
            connection = new NioClient(getAgentName(), host, shell.getPort(), shell.getWorkers(),
                    shell.getSslHandshakeTimeout(), this);
            logger.info("Connecting to host: {}", host);
            try {
                connection.start();
            } catch (final NioConnectionException e) {
                stopAndCleanupConnection(false);
                logger.info("Attempted to connect to the server, but received an unexpected exception, trying again...", e);
            }
        }
        shell.updateConnectedHost(((NioClient)connection).getHost());
        scavengeOldAgentObjects();
    }

    public void stop(final String reason, final String detail) {
        logger.info("Stopping the agent: Reason = {}{}", reason, (detail != null ? ": Detail = " + detail : ""));
        reconnectAllowed = false;
        if (connection != null) {
            final ShutdownCommand cmd = new ShutdownCommand(reason, detail);
            try {
                if (link != null) {
                    final Request req = new Request(id != null ? id : -1, -1, cmd, false);
                    link.send(req.toBytes());
                }
            } catch (final ClosedChannelException e) {
                logger.warn("Unable to send: {}", cmd.toString());
            } catch (final Exception e) {
                logger.warn("Unable to send: {} due to exception: {}", cmd.toString(), e);
            }
            logger.debug("Sending shutdown to management server");
            try {
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
                logger.debug("Who the heck interrupted me here?");
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
        logger.debug("Set agent id {}", id);
        this.id = id;
        shell.setPersistentProperty(getResourceName(), "id", Long.toString(id));
    }

    public String getUuid() {
        return _uuid;
    }

    public void setUuid(String uuid) {
        this._uuid = uuid;
        shell.setPersistentProperty(getResourceName(), "uuid", uuid);
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        this._name = name;
        shell.setPersistentProperty(getResourceName(), "name", name);
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
                logger.debug("Forcing {} shutdown as it did not shutdown in the desired time due to: {}",
                                name, e.getMessage());
                certExecutor.shutdownNow();
            }
        }
        certExecutor = Executors.newSingleThreadScheduledExecutor((new NamedThreadFactory(name)));
        certExecutor.schedule(new PostCertificateRenewalTask(this), 5, TimeUnit.SECONDS);
    }

    private void scheduleHostLBCheckerTask(final long checkInterval) {
        String name = "HostLBCheckerTask";
        if (hostLbCheckExecutor != null && !hostLbCheckExecutor.isShutdown()) {
            hostLbCheckExecutor.shutdown();
            try {
                if (!hostLbCheckExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    hostLbCheckExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                logger.debug("Forcing {} shutdown as it did not shutdown in the desired time due to: {}",
                        name, e.getMessage());
                hostLbCheckExecutor.shutdownNow();
            }
        }
        if (checkInterval > 0L) {
            logger.info("Scheduling preferred host task with host.lb.interval={}ms", checkInterval);
            hostLbCheckExecutor = Executors.newSingleThreadScheduledExecutor((new NamedThreadFactory(name)));
            hostLbCheckExecutor.scheduleAtFixedRate(new PreferredHostCheckerTask(), checkInterval, checkInterval,
                    TimeUnit.MILLISECONDS);
        }
    }

    public void scheduleWatch(final Link link, final Request request, final long delay, final long period) {
        logger.debug("Adding a watch list");
        final WatchTask task = new WatchTask(link, request, this);
        final ScheduledFuture<?> future = selfTaskExecutor.scheduleAtFixedRate(task, delay, period, TimeUnit.MILLISECONDS);
        watchList.add(future);
    }

    public void triggerUpdate() {
        PingCommand command = serverResource.getCurrentStatus(getId());
        command.setOutOfBand(true);
        logger.debug("Sending out of band ping");
        final Request request = new Request(id, -1, command, false);
        request.setSequence(getNextSequence());
        try {
            link.send(request.toBytes());
        } catch (final ClosedChannelException e) {
            logger.warn("Unable to send ping update: {}", request.toString());
        }
    }

    protected void cancelTasks() {
        for (final ScheduledFuture<?> task : watchList) {
            task.cancel(true);
        }
        logger.debug("Clearing watch list: {}", () -> watchList.size());
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
        logger.debug("Creating startup task for link: {}", () -> getLinkLog(link));
        StartupTask currentTask = startupTask.get();
        if (currentTask != null) {
            logger.warn("A Startup task is already locked or in progress, cannot create for link {}",
                    getLinkLog(link));
            return;
        }
        currentTask = new StartupTask(link);
        if (startupTask.compareAndSet(null, currentTask)) {
            selfTaskExecutor.schedule(currentTask, startupWait, TimeUnit.SECONDS);
            return;
        }
        logger.warn("Failed to lock a StartupTask for link: {}", getLinkLog(link));
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
        sendStartup(link, false);
    }

    public void sendStartup(final Link link, boolean transfer) {
        final StartupCommand[] startup = serverResource.initialize();
        if (startup != null) {
            final String msHostList = shell.getPersistentProperty(null, "host");
            final Command[] commands = new Command[startup.length];
            for (int i = 0; i < startup.length; i++) {
                setupStartupCommand(startup[i]);
                startup[i].setMSHostList(msHostList);
                startup[i].setConnectionTransferred(transfer);
                commands[i] = startup[i];
            }
            final Request request = new Request(id != null ? id : -1, -1, commands, false, false);
            request.setSequence(getNextSequence());

            logger.debug("Sending Startup: {}", request.toString());
            lockStartupTask(link);
            try {
                link.send(request.toBytes());
            } catch (final ClosedChannelException e) {
                logger.warn("Unable to send request to {} due to '{}', request: {}",
                        getLinkLog(link), e.getMessage(), request);
            }

            if (serverResource instanceof ResourceStatusUpdater) {
                ((ResourceStatusUpdater) serverResource).registerStatusUpdater(this);
            }
        }
    }

    protected String retrieveHostname() {
        logger.trace("Retrieving hostname with resource={}", () -> serverResource.getClass().getSimpleName());
        final String result = Script.runSimpleBashScript(Script.getExecutableAbsolutePath("hostname"), 500);
        if (StringUtils.isNotBlank(result)) {
            return result;
        }
        try {
            InetAddress address = InetAddress.getLocalHost();
            return address.toString();
        } catch (final UnknownHostException e) {
            logger.warn("unknown host? ", e);
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
        startup.setArch(getAgentArch());
    }

    protected String getAgentArch() {
        final Script command = new Script("/usr/bin/arch", 500, logger);
        final OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
        return command.execute(parser);
    }

    @Override
    public Task create(final Task.Type type, final Link link, final byte[] data) {
        return new ServerHandler(type, link, data);
    }

    protected void reconnect(final Link link) {
        reconnect(link, null, false);
    }

    protected void reconnect(final Link link, String preferredMSHost, boolean forTransfer) {
        if (!(forTransfer || reconnectAllowed)) {
            logger.debug("Reconnect requested but it is not allowed {}", () -> getLinkLog(link));
            return;
        }
        cancelStartupTask();
        closeAndTerminateLink(link);
        closeAndTerminateLink(this.link);
        setLink(null);
        cancelTasks();
        serverResource.disconnected();
        logger.info("Lost connection to host: {}. Attempting reconnection while we still have {} commands in progress.", shell.getConnectedHost(), commandsInProgress.get());
        stopAndCleanupConnection(true);
        String host = preferredMSHost;
        if (org.apache.commons.lang3.StringUtils.isBlank(host)) {
            host = shell.getNextHost();
        }
        List<String> avoidMSHostList = shell.getAvoidHosts();
        do {
            if (CollectionUtils.isEmpty(avoidMSHostList) || !avoidMSHostList.contains(host)) {
                connection = new NioClient(getAgentName(), host, shell.getPort(), shell.getWorkers(), shell.getSslHandshakeTimeout(), this);
                logger.info("Reconnecting to host: {}", host);
                try {
                    connection.start();
                } catch (final NioConnectionException e) {
                    logger.info("Attempted to re-connect to the server, but received an unexpected exception, trying again...", e);
                    stopAndCleanupConnection(false);
                }
            }
            shell.getBackoffAlgorithm().waitBeforeRetry();
            host = shell.getNextHost();
        } while (!connection.isStartup());
        shell.updateConnectedHost(((NioClient)connection).getHost());
        logger.info("Connected to the host: {}", shell.getConnectedHost());
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
            logger.warn("Fail to clean up old connection. {}", e);
        }
        if (!waitForStop) {
            return;
        }
        do {
            shell.getBackoffAlgorithm().waitBeforeRetry();
        } while (connection.isStartup());
    }

    public void processStartupAnswer(final Answer answer, final Response response, final Link link) {
        boolean answerValid = cancelStartupTask();
        final StartupAnswer startup = (StartupAnswer)answer;
        if (!startup.getResult()) {
            logger.error("Not allowed to connect to the server: {}", answer.getDetails());
            if (serverResource != null && !serverResource.isExitOnFailures()) {
                logger.trace("{} does not allow exit on failure, reconnecting",
                        serverResource.getClass().getSimpleName());
                reconnect(link);
                return;
            }
            System.exit(1);
        }
        if (!answerValid) {
            logger.warn("Threw away a startup answer because we're reconnecting.");
            return;
        }

        logger.info("Process agent startup answer, agent [id: {}, uuid: {}, name: {}] connected to the server",
                startup.getHostId(), startup.getHostUuid(), startup.getHostName());

        setId(startup.getHostId());
        setUuid(startup.getHostUuid());
        setName(startup.getHostName());
        pingInterval = startup.getPingInterval() * 1000L; // change to ms.

        updateLastPingResponseTime();
        scheduleWatch(link, response, pingInterval, pingInterval);

        outRequestHandler.setKeepAliveTime(2 * pingInterval, TimeUnit.MILLISECONDS);

        logger.info("Startup Response Received: agent [id: {}, uuid: {}, name: {}]",
                startup.getHostId(), startup.getHostUuid(), startup.getHostName());
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
                        ThreadContext.put("logcontextid", cmd.getContextParam("logid"));
                    }
                    if (logger.isDebugEnabled()) {
                        if (!requestLogged) // ensures request is logged only once per method call
                        {
                            final String requestMsg = request.toString();
                            if (requestMsg != null) {
                                logger.debug("Request:{}",requestMsg);
                            }
                            requestLogged = true;
                        }
                        logger.debug("Processing command: {}", cmd.toString());
                    }

                    if (cmd instanceof CronCommand) {
                        final CronCommand watch = (CronCommand)cmd;
                        scheduleWatch(link, request, watch.getInterval() * 1000L, watch.getInterval() * 1000L);
                        answer = new Answer(cmd, true, null);
                    } else if (cmd instanceof ShutdownCommand) {
                        final ShutdownCommand shutdown = (ShutdownCommand)cmd;
                        logger.debug("Received shutdownCommand, due to: {}", shutdown.getReason());
                        cancelTasks();
                        if (shutdown.isRemoveHost()) {
                            cleanupAgentZoneProperties();
                        }
                        reconnectAllowed = false;
                        answer = new Answer(cmd, true, null);
                    } else if (cmd instanceof ReadyCommand && ((ReadyCommand)cmd).getDetails() != null) {

                        logger.debug("Not ready to connect to mgt server: {}", ((ReadyCommand)cmd).getDetails());
                        if (serverResource != null && !serverResource.isExitOnFailures()) {
                            logger.trace("{} does not allow exit on failure, reconnecting",
                                    serverResource.getClass().getSimpleName());
                            reconnect(link);
                            return;
                        }
                        System.exit(1);
                        return;
                    } else if (cmd instanceof MaintainCommand) {
                        logger.debug("Received maintainCommand, do not cancel current tasks");
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
                            logger.warn("No handler found to process cmd: {}", cmd.toString());
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
                    } else if (cmd instanceof MigrateAgentConnectionCommand) {
                        answer = migrateAgentToOtherMS((MigrateAgentConnectionCommand) cmd);
                    } else {
                        if (cmd instanceof ReadyCommand) {
                            processReadyCommand(cmd);
                        }
                        commandsInProgress.incrementAndGet();
                        try {
                            if (cmd.isReconcile()) {
                                cmd.setRequestSequence(request.getSequence());
                            }
                            answer = serverResource.executeRequest(cmd);
                        } finally {
                            commandsInProgress.decrementAndGet();
                        }
                        if (answer == null) {
                            logger.debug("Response: unsupported command {}", cmd.toString());
                            answer = Answer.createUnsupportedCommandAnswer(cmd);
                        }
                    }
                } catch (final Throwable th) {
                    logger.warn("Caught: ", th);
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
            if (logger.isDebugEnabled()) {
                final String responseMsg = response.toString();
                if (responseMsg != null) {
                    logger.debug(response.toString());
                }
            }

            if (response != null) {
                try {
                    link.send(response.toBytes());
                } catch (final ClosedChannelException e) {
                    logger.warn("Unable to send response: {}", response.toString());
                }
            }
        }
    }

    public Answer setupAgentKeystore(final SetupKeyStoreCommand cmd) {
        final String keyStorePassword = cmd.getKeystorePassword();
        final long validityDays = cmd.getValidityDays();

        logger.debug("Setting up agent keystore file and generating CSR");

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

        Script script = new Script(keystoreSetupSetupPath, 300000, logger);
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

        logger.debug("Importing received certificate to agent's keystore");

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
            logger.debug("Saved received client certificate to: {}", certFile);
        } catch (IOException e) {
            throw new CloudRuntimeException("Unable to save received agent client and ca certificates", e);
        }

        String ksPassphrase = shell.getPersistentProperty(null, KeyStoreUtils.KS_PASSPHRASE_PROPERTY);
        Script script = new Script(keystoreCertImportScriptPath, 300000, logger);
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

    private void processManagementServerList(final List<String> msList, final List<String> avoidMsList, final String lbAlgorithm, final Long lbCheckInterval) {
        if (CollectionUtils.isNotEmpty(msList) && StringUtils.isNotEmpty(lbAlgorithm)) {
            try {
                final String newMSHosts = String.format("%s%s%s", com.cloud.utils.StringUtils.toCSVList(msList), IAgentShell.hostLbAlgorithmSeparator, lbAlgorithm);
                shell.setPersistentProperty(null, "host", newMSHosts);
                shell.setHosts(newMSHosts);
                shell.resetHostCounter();
                logger.info("Processed new management server list: {}", newMSHosts);
            } catch (final Exception e) {
                throw new CloudRuntimeException("Could not persist received management servers list", e);
            }
        }
        shell.setAvoidHosts(avoidMsList);
        if ("shuffle".equals(lbAlgorithm)) {
            scheduleHostLBCheckerTask(0);
        } else {
            scheduleHostLBCheckerTask(shell.getLbCheckerInterval(lbCheckInterval));
        }
    }

    private Answer setupManagementServerList(final SetupMSListCommand cmd) {
        processManagementServerList(cmd.getMsList(), cmd.getAvoidMsList(), cmd.getLbAlgorithm(), cmd.getLbCheckInterval());
        return new SetupMSListAnswer(true);
    }

    private Answer migrateAgentToOtherMS(final MigrateAgentConnectionCommand cmd) {
        try {
            if (CollectionUtils.isNotEmpty(cmd.getMsList())) {
                processManagementServerList(cmd.getMsList(), cmd.getAvoidMsList(), cmd.getLbAlgorithm(), cmd.getLbCheckInterval());
            }
            Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("MigrateAgentConnection-Job")).schedule(() -> {
                migrateAgentConnection(cmd.getAvoidMsList());
            }, 3, TimeUnit.SECONDS);
        } catch (Exception e) {
            String errMsg = "Migrate agent connection failed, due to " + e.getMessage();
            logger.debug(errMsg, e);
            return new MigrateAgentConnectionAnswer(errMsg);
        }
        return new MigrateAgentConnectionAnswer(true);
    }

    private void migrateAgentConnection(List<String> avoidMsList) {
        final String[] msHosts = shell.getHosts();
        if (msHosts == null || msHosts.length < 1) {
            throw new CloudRuntimeException("Management Server hosts empty, not properly configured in agent");
        }

        List<String> msHostsList = new ArrayList<>(Arrays.asList(msHosts));
        msHostsList.removeAll(avoidMsList);
        if (msHostsList.isEmpty() || StringUtils.isEmpty(msHostsList.get(0))) {
            throw new CloudRuntimeException("No other Management Server hosts to migrate");
        }

        String preferredMSHost  = null;
        for (String msHost : msHostsList) {
            try (final Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(msHost, shell.getPort()), 5000);
                preferredMSHost = msHost;
                break;
            } catch (final IOException e) {
                throw new CloudRuntimeException("Management server host: " + msHost + " is not reachable, to migrate connection");
            }
        }

        if (preferredMSHost == null) {
            throw new CloudRuntimeException("Management server host(s) are not reachable, to migrate connection");
        }

        logger.debug("Management server host " + preferredMSHost + " is found to be reachable, trying to reconnect");
        shell.resetHostCounter();
        shell.setAvoidHosts(avoidMsList);
        shell.setConnectionTransfer(true);
        reconnect(link, preferredMSHost, true);
    }

    public void processResponse(final Response response, final Link link) {
        final Answer answer = response.getAnswer();
        logger.debug("Received response: {}", response.toString());
        if (answer instanceof StartupAnswer) {
            processStartupAnswer(answer, response, link);
        } else if (answer instanceof AgentControlAnswer) {
            // Notice, we are doing callback while holding a lock!
            for (final IAgentControlListener listener : controlListeners) {
                listener.processControlResponse(response, (AgentControlAnswer)answer);
            }
        } else if (answer instanceof PingAnswer) {
            processPingAnswer((PingAnswer) answer);
        } else {
            updateLastPingResponseTime();
        }
    }

    private void processPingAnswer(final PingAnswer answer) {
        if ((answer.isSendStartup()) && reconnectAllowed) {
            logger.info("Management server requested startup command to reinitialize the agent");
            sendStartup(link);
        } else {
            serverResource.processPingAnswer((PingAnswer) answer);
        }
        shell.setAvoidHosts(answer.getAvoidMsList());
    }

    public void processReadyCommand(final Command cmd) {
        final ReadyCommand ready = (ReadyCommand)cmd;
        // Set human readable sizes;
        Boolean humanReadable = ready.getEnableHumanReadableSizes();
        if (humanReadable != null){
            NumbersUtil.enableHumanReadableSizes = humanReadable;
        }

        logger.info("Processing agent ready command, agent id = {}, uuid = {}, name = {}", ready.getHostId(), ready.getHostUuid(), ready.getHostName());
        if (ready.getHostId() != null) {
            setId(ready.getHostId());
            setUuid(ready.getHostUuid());
            setName(ready.getHostName());
        }

        verifyAgentArch(ready.getArch());
        processManagementServerList(ready.getMsHostList(), ready.getAvoidMsHostList(), ready.getLbAlgorithm(), ready.getLbCheckInterval());

        logger.info("Ready command is processed for agent [id: {}, uuid: {}, name: {}]", getId(), getUuid(), getName());
    }

    private void verifyAgentArch(String arch) {
        if (StringUtils.isNotBlank(arch)) {
            String agentArch = getAgentArch();
            if (!arch.equals(agentArch)) {
                logger.error("Unexpected arch {}, expected {}", agentArch, arch);
            }
        }
    }

    public void processOtherTask(final Task task) {
        final Object obj = task.get();
        if (obj instanceof Response) {
            if (System.currentTimeMillis() - lastPingResponseTime.get() > pingInterval * shell.getPingRetries()) {
                logger.error("Ping Interval has gone past {}. Won't reconnect to mgt server, as connection is still alive",
                        pingInterval * shell.getPingRetries());
                return;
            }

            final PingCommand ping = serverResource.getCurrentStatus(getId());
            final Request request = new Request(id, -1, ping, false);
            request.setSequence(getNextSequence());
            logger.debug("Sending ping: {}", request.toString());

            try {
                task.getLink().send(request.toBytes());
                //if i can send pingcommand out, means the link is ok
                updateLastPingResponseTime();
            } catch (final ClosedChannelException e) {
                logger.warn("Unable to send request to {} due to '{}', request: {}",
                        getLinkLog(task.getLink()), e.getMessage(), request);
            }

        } else if (obj instanceof Request) {
            final Request req = (Request)obj;
            final Command command = req.getCommand();
            if (command.getContextParam("logid") != null) {
                ThreadContext.put("logcontextid", command.getContextParam("logid"));
            }
            Answer answer = null;
            commandsInProgress.incrementAndGet();
            try {
                if (command.isReconcile()) {
                    command.setRequestSequence(req.getSequence());
                }
                answer = serverResource.executeRequest(command);
            } finally {
                commandsInProgress.decrementAndGet();
            }
            if (answer != null) {
                final Response response = new Response(req, answer);

                logger.debug("Watch Sent: {}", response.toString());
                try {
                    task.getLink().send(response.toBytes());
                } catch (final ClosedChannelException e) {
                    logger.warn("Unable to send response: {}", response.toString());
                }
            }
        } else {
            logger.warn("Ignoring an unknown task");
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
            synchronized (listener) {
                try {
                    listener.wait(timeoutInMilliseconds);
                } catch (final InterruptedException e) {
                    logger.warn("sendRequest is interrupted, exit waiting");
                }
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
                logger.warn("Unable to post agent control request: {}", request.toString());
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

    public class WatchTask implements Runnable {
        protected Request _request;
        protected Agent _agent;
        protected Link link;

        public WatchTask(final Link link, final Request request, final Agent agent) {
            super();
            _request = request;
            this.link = link;
            _agent = agent;
        }

        @Override
        public void run() {
            logger.trace("Scheduling {}", (_request instanceof Response ? "Ping" : "Watch Task"));
            try {
                if (_request instanceof Response) {
                    outRequestHandler.submit(new ServerHandler(Task.Type.OTHER, link, _request));
                } else {
                    link.schedule(new ServerHandler(Task.Type.OTHER, link, _request));
                }
            } catch (final ClosedChannelException e) {
                logger.warn("Unable to schedule task because channel is closed");
            }
        }
    }

    public class StartupTask implements Runnable {
        protected Link link;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        public StartupTask(final Link link) {
            logger.debug("Startup task created");
            this.link = link;
        }

        public boolean cancel() {
            // TimerTask.cancel may fail depends on the calling context
            if (cancelled.compareAndSet(false, true)) {
                startupWait = DEFAULT_STARTUP_WAIT;
                logger.debug("Startup task cancelled");
            }
            return true;
        }

        @Override
        public void run() {
            if (cancelled.compareAndSet(false, true)) {
                logger.info("The running startup command is now invalid. Attempting reconnect");
                startupTask.set(null);
                startupWait = DEFAULT_STARTUP_WAIT * 2;
                logger.debug("Executing reconnect from task - {}", () -> getLinkLog(link));
                reconnect(link);
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
                sendStartup(task.getLink(), shell.isConnectionTransfer());
                shell.setConnectionTransfer(false);
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
                    logger.error("Unable to find this request ");
                } catch (final Exception e) {
                    logger.error("Error parsing task", e);
                }
            } else if (task.getType() == Task.Type.DISCONNECT) {
                try {
                    // an issue has been found if reconnect immediately after disconnecting. please refer to https://github.com/apache/cloudstack/issues/8517
                    // wait 5 seconds before reconnecting
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
                shell.setConnectionTransfer(false);
                logger.debug("Executing disconnect task - {}", () -> getLinkLog(task.getLink()));
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
                        logger.debug("Running post certificate renewal task to restart services.");

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
                        agent.shell = null;
                        agent.watchList = null;
                        agent.shutdownThread = null;
                        agent.controlListeners = null;
                        agent = null;

                        // Start a new agent instance
                        shell.launchNewAgent(resource);
                        return;
                    }
                    logger.debug("Other tasks are in progress, will retry post certificate renewal command after few seconds");
                    Thread.sleep(5000);
                } catch (final Exception e) {
                    logger.warn("Failed to execute post certificate renewal command:", e);
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
                final String preferredMSHost  = msList[0];
                final String connectedHost = shell.getConnectedHost();
                logger.debug("Running preferred host checker task, connected host={}, preferred host={}",
                        connectedHost, preferredMSHost);
                if (preferredMSHost == null || preferredMSHost.equals(connectedHost) || link == null) {
                    return;
                }
                boolean isHostUp = false;
                try (final Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(preferredMSHost, shell.getPort()), 5000);
                    isHostUp = true;
                } catch (final IOException e) {
                    logger.debug("Host: {} is not reachable", preferredMSHost);
                }
                if (isHostUp && link != null && commandsInProgress.get() == 0) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Preferred host {} is found to be reachable, trying to reconnect", preferredMSHost);
                    }
                    shell.resetHostCounter();
                    reconnect(link, preferredMSHost, false);
                }
            } catch (Throwable t) {
                logger.error("Error caught while attempting to connect to preferred host", t);
            }
        }
    }
}
