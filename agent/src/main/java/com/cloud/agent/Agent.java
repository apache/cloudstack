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
import java.net.ConnectException;
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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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

import com.cloud.agent.api.AgentConnectStatusAnswer;
import com.cloud.agent.api.AgentConnectStatusCommand;
import com.cloud.agent.properties.AgentProperties;
import com.cloud.utils.LogUtils;
import com.cloud.utils.UuidUtils;
import com.cloud.utils.backoff.BackoffFactory;
import com.cloud.utils.net.NetUtils;
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
import org.apache.commons.lang3.ArrayUtils;
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
import com.cloud.resource.ServerResource;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.StringUtils;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.NioConnectionException;
import com.cloud.utils.nio.HandlerFactory;
import com.cloud.utils.nio.Link;
import com.cloud.utils.nio.NioClient;
import com.cloud.utils.nio.NioConnection;
import com.cloud.utils.nio.Task;
import com.cloud.utils.script.Script;

import static com.cloud.agent.HostConnectProcess.DEFAULT_ASYNC_COMMAND_TIMEOUT_SEC;

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

    private static final int HOST_SOCKET_CONNECT_TIMEOUT_MS = 5000;

    /**
     * Constant to verify {@link ConnectException} cause.
     */
    private static final String CONNECTION_REFUSED_MSG = "Connection refused";

    /**
     * Constant to verify "probably rejected by MS due to {@link NioConnection#rejectConnectionIfBusy}".
     */
    private static final String BROKEN_PIPE_MSG = "Broken pipe";

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

    ScheduledExecutorService watchTaskExecutor;
    ScheduledExecutorService certExecutor;
    ScheduledExecutorService hostLbCheckExecutor;

    CopyOnWriteArrayList<ScheduledFuture<?>> watchList = new CopyOnWriteArrayList<>();
    AtomicLong sequence = new AtomicLong(0);
    AtomicLong lastPingResponseTime = new AtomicLong(0L);
    long pingInterval = 0;
    AtomicInteger commandsInProgress = new AtomicInteger(0);

    private static final AtomicBoolean RECONNECT_LOCK = new AtomicBoolean(false);
    boolean reconnectAllowed = true;

    //For time sensitive task, e.g. PingTask
    ThreadPoolExecutor outRequestHandler;
    ExecutorService requestHandler;

    Thread shutdownThread = new ShutdownThread(this);

    private String keystoreSetupSetupPath;
    private String keystoreCertImportScriptPath;

    String hostname;

    HostConnectProcess hostConnectProcess = new HostConnectProcess(this);

    public ExecutorService getRequestHandler() {
        return requestHandler;
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
        watchTaskExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Agent-WatchTask"));
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
        setupShutdownHookAndInitExecutors();
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
        ThreadContext.put("logcontextid", UuidUtils.first(UUID.randomUUID().toString()));
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

        reconnect(null, null, false);

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
                logger.warn("Unable to send: {}", cmd);
            } catch (final Exception e) {
                logger.warn("Unable to send: {} due to exception: {}", cmd, e);
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

        if (outRequestHandler != null) {
            outRequestHandler.shutdownNow();
            outRequestHandler = null;
        }

        if (requestHandler != null) {
            requestHandler.shutdown();
            requestHandler = null;
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

    private void scheduleHostLBCheckerTask(final String lbAlgorithm, final long checkInterval) {
        String name = "HostLBCheckerTask";
        if (hostLbCheckExecutor != null && !hostLbCheckExecutor.isShutdown()) {
            logger.info("Shutting down the preferred host checker task {}", name);
            hostLbCheckExecutor.shutdown();
            try {
                if (!hostLbCheckExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    hostLbCheckExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                logger.debug("Forcing the preferred host checker task {} shutdown as it did not shutdown in the desired time due to: {}",
                        name, e.getMessage());
                hostLbCheckExecutor.shutdownNow();
            }
        }
        if (checkInterval > 0L) {
            hostLbCheckExecutor = Executors.newSingleThreadScheduledExecutor((new NamedThreadFactory(name)));
            if ("shuffle".equalsIgnoreCase(lbAlgorithm)) {
                logger.info("Scheduling the preferred host checker task to trigger once (to apply lb algorithm '{}') after host.lb.interval={} ms", lbAlgorithm, checkInterval);
                hostLbCheckExecutor.schedule(new PreferredHostCheckerTask(), checkInterval, TimeUnit.MILLISECONDS);
            } else {
                logger.info("Scheduling a recurring preferred host checker task with host.lb.interval={} ms", checkInterval);
                hostLbCheckExecutor.scheduleAtFixedRate(new PreferredHostCheckerTask(), checkInterval, checkInterval, TimeUnit.MILLISECONDS);
            }
        }
    }

    public void scheduleWatch(final Link link, final Request request, final long delay, final long period) {
        logger.debug("Adding a watch list");
        final WatchTask task = new WatchTask(link, request, this);
        final ScheduledFuture<?> future = watchTaskExecutor.scheduleAtFixedRate(task, delay, period, TimeUnit.MILLISECONDS);
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
            logger.warn("Unable to send ping update: {}", request);
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

    protected AgentConnectStatusCommand setupAgentConnectStatusCommand(AgentConnectStatusCommand command) {
        // there is requirement in Response class to have id as long, to avoid null pointer during boxing, let's set -1 here
        command.setHostId(Optional.ofNullable(getId()).orElse(-1L));
        if (StringUtils.isBlank(hostname)) {
            hostname = retrieveHostname();
        }
        command.setHostName(hostname);
        command.setHostGuid(getResourceGuid());
        return command;
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
        String arch = Script.runSimpleBashScript(Script.getExecutableAbsolutePath("arch"), 2000);
        logger.debug("Arch for agent: {} found: {}", _name, arch);
        return arch;
    }

    @Override
    public Task create(final Task.Type type, final Link link, final byte[] data) {
        return new ServerHandler(type, link, data);
    }

    protected void closeAndTerminateLink(Link link) {
        Optional.ofNullable(link)
                .map(Link::attachment)
                .filter(ServerAttache.class::isInstance)
                .map(ServerAttache.class::cast)
                .ifPresentOrElse(ServerAttache::disconnect, () -> {
                    if (link != null) {
                        link.close();
                        link.terminated();
                    }
                });
    }

    protected void stopAndCleanupConnection() {
        if (connection == null) {
            return;
        }
        NioConnection connection = this.connection;
        connection.stop();
        try {
            connection.cleanUp();
        } catch (final IOException e) {
            logger.warn("Fail to clean up old connection", e);
        }

        try {
            while (connection.isStartup()) {
                logger.debug("Waiting for connection graceful stop");
                shell.getBackoffAlgorithm().waitBeforeRetry();
                connection.stop();
            }
        } catch (Exception e) {
            logger.warn("Failed to gracefully stop connection", e);
        }
        logger.debug("Connection stopped");
    }

    /**
     * Select the host to reconnect to based on priority:
     * 1. preferredHost if defined and not blank
     * 2. Link's socket address IP if available and not null
     * 3. shell.getNextHost() if the above two options are not met
     *
     * @param preferredHost the preferred host to connect to
     * @param link the current link which may contain socket address information
     * @return the host to connect to
     */
    protected String selectReconnectionHost(String preferredHost, Link link) {
        return Optional.ofNullable(preferredHost)
                .filter(org.apache.commons.lang3.StringUtils::isNotBlank)
                .orElseGet(() -> Optional.ofNullable(link)
                        .map(Link::getSocketAddress)
                        .map(InetSocketAddress::getAddress)
                        .map(InetAddress::getHostAddress)
                        .orElseGet(shell::getNextHost));
    }

    /**
     * Reconnect to Management Server.
     *
     * @param link           - connection holder
     * @param preferredHost  - if defined, reconnect will be performed to this Host first,
     *                       otherwise will be used {@link IAgentShell#getNextHost()}
     * @param forceReconnect - expected to be true if called by {@link MigrateAgentConnectionCommand},
     *                       this is only "switch Management Server", it does not perform full Host Connect process.
     */
    protected void reconnect(Link link, String preferredHost, boolean forceReconnect) {
        if (!RECONNECT_LOCK.compareAndSet(false, true)) {
            logger.warn("Reconnect is already running, exiting");
            return;
        }
        String requestedLink = Optional.ofNullable(link).map(Link::toString).orElse("N/A");
        String currentLink = Optional.ofNullable(this.link).map(Link::toString).orElse("N/A");
        logger.info("Reconnect info: provided link: {}, agent link: {}, preferred host: {}, force" +
                " reconnect: {}", requestedLink, currentLink, preferredHost, forceReconnect);

        try {
            logger.debug("Obtained reconnect lock");
            if (!(forceReconnect || reconnectAllowed)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Reconnect requested but it is not allowed {}", link);
                }
                return;
            }

            if (isReconnectStormDetected(link, preferredHost, requestedLink, currentLink)) {
                return;
            }

            cleanupConnectionBeforeReconnect(link);
            // start with preferred host
            String host = selectReconnectionHost(preferredHost, link);

            String hostLog = LogUtils.getHostLog(host, shell.getPort());
            List<String> avoidMsHostList = Optional.ofNullable(shell.getAvoidHosts()).orElseGet(List::of);
            // pointer to the first element of "refuse loop"
            AtomicReference<String> firstRefuseLoopHostRef = new AtomicReference<>(null);
            // to break deadlock where "non-avoid" MS Hosts are down and only "avoid" are up
            AtomicBoolean ignoreAvoidMsHostListRef = new AtomicBoolean(false);
            do {
                AtomicBoolean skipTimeoutRef = new AtomicBoolean(false);
                String parentLogContextId = (String) ThreadContext.get("logcontextid");
                if (parentLogContextId != null) {
                    ThreadContext.put("logcontextid-parent", parentLogContextId);
                }
                ThreadContext.put("logcontextid", UuidUtils.first(UUID.randomUUID().toString()));
                if (ignoreAvoidMsHostListRef.get() || !avoidMsHostList.contains(host)) {
                    connection = new NioClient(getAgentName(), host, shell.getPort(), shell.getWorkers(),
                            shell.getSslHandshakeTimeout(), this);
                    logger.info("Reconnecting to host: {}", hostLog);
                    try {
                        connection.start();
                        // successfully connected, skip the rest
                        continue;
                    } catch (Exception e) {
                        logReconnectionFailure(e, hostLog);

                        try {
                            stopAndCleanupConnection();
                        } catch (Exception ex) {
                            logger.warn("Got an exception during stop and cleanup connection", e);
                        }

                        updateRefuseLoopState(e, host, firstRefuseLoopHostRef, ignoreAvoidMsHostListRef, skipTimeoutRef);
                    }
                } else {
                    logger.debug("Next host {} is in avoid list, skipped", hostLog);
                    if (org.apache.commons.lang3.StringUtils.isBlank(preferredHost)) {
                        logHostLists(avoidMsHostList);
                        skipTimeoutRef.set(true);
                    }
                }
                if(!skipTimeoutRef.get()) {
                    shell.getBackoffAlgorithm().waitBeforeRetry();
                }
                host = shell.getNextHost();
                hostLog = LogUtils.getHostLog(host, shell.getPort());
                logger.debug("Next host to connect: {}", hostLog);
            } while (!connection.isStartup());
            // successfully connected
            shell.updateConnectedHost(((NioClient) connection).getHost());
            String msg = String.format("Connected to the host: %s (%s)", shell.getConnectedHost(), this.link);
            logger.info(msg);
        } finally {
            RECONNECT_LOCK.set(false);
            logger.debug("Removed reconnect lock");
        }
    }

    /**
     * Handles "Connection refused" loop detection and determines if backoff timeout should be skipped.
     * Manages refuse loop state to detect when all management servers have been tried and need
     * to ignore avoid list to prevent deadlock.
     *
     * @param e the exception from connection attempt
     * @param host the current host being attempted
     * @param firstRefuseLoopHostRef reference to first host in refuse loop (modified by this method)
     * @param ignoreAvoidMsHostListRef flag to ignore avoid list (modified by this method)
     * @return true if timeout should be skipped (connection refused), false otherwise
     */
    private void updateRefuseLoopState(Exception e, String host, AtomicReference<String> firstRefuseLoopHostRef, AtomicBoolean ignoreAvoidMsHostListRef, AtomicBoolean skipTimeoutRef) {
        // we are skipping timeout for "Connection refused" to not waste time on down MS
        boolean skipTimeout = Optional.ofNullable(e.getCause())
                .filter(ConnectException.class::isInstance)
                .map(Throwable::getMessage)
                .filter(CONNECTION_REFUSED_MSG::equalsIgnoreCase)
                .isPresent();
        skipTimeoutRef.set(skipTimeout);
        String firstRefuseLoopHost = firstRefuseLoopHostRef.get();
        // for each "Connection refused" (maybe need to have a copy of variable with better name)
        // start "refuse loop"
        if (skipTimeout && firstRefuseLoopHost == null) {
            firstRefuseLoopHostRef.set(host);
            ignoreAvoidMsHostListRef.set(false);
            logger.debug("Started refuse loop for host {}", firstRefuseLoopHost);
            // closed "refuse loop"
        } else if (skipTimeout && firstRefuseLoopHost.equalsIgnoreCase(host)) {
            ignoreAvoidMsHostListRef.set(true);
            logger.debug("Closed refuse loop for host {}", firstRefuseLoopHost);
            // got non "refuse" related issue, break "refuse loop"
        } else if (!skipTimeout && (firstRefuseLoopHostRef != null || ignoreAvoidMsHostListRef.get())) {
            logger.debug("Broke refuse loop for host {} by {}", firstRefuseLoopHost, host);
            firstRefuseLoopHostRef.set(null);
            ignoreAvoidMsHostListRef.set(false);
        }
    }

    /**
     * Logs reconnection failure with appropriate level based on rejection reason.
     * If connection was rejected due to max concurrent connections limit (Broken pipe),
     * logs as warning. Otherwise logs as info.
     *
     * @param e the exception that occurred during reconnection attempt
     * @param hostLog the formatted host log string for logging
     */
    private void logReconnectionFailure(Exception e, String hostLog) {
        // check if got NIO Connection exception, caused by IO Exception "Broken pipe"
        boolean rejectedByMs = Optional.of(e).filter(NioConnectionException.class::isInstance)
                .map(Exception::getCause)
                .filter(IOException.class::isInstance)
                .map(IOException.class::cast)
                .map(IOException::getMessage)
                .filter(BROKEN_PIPE_MSG::equalsIgnoreCase)
                .isPresent();
        if(rejectedByMs) {
            logger.warn("Attempted to re-connect to {}, but rejected" +
                    " due to 'agent.max.concurrent.new.connections' reached limit," +
                    " will try again", hostLog, e);
        } else {
            logger.info("Attempted to re-connect to {}, but got exception," +
                    " will try again", hostLog, e);
        }
    }

    /**
     * Logs all management server host lists for debugging reconnection logic.
     * Outputs defined hosts, hosts to avoid, and calculated available hosts.
     *
     * @param avoidMsHostList list of management server hosts to avoid during reconnection
     */
    private void logHostLists(List<String> avoidMsHostList) {
        logger.debug("Preferred host is not defined");
        try {
            List<String> hostsList = Optional.ofNullable(shell.getHosts())
                    .map(Arrays::asList)
                    .orElseGet(List::of);

            List<String> hostsShortList = new ArrayList<>(hostsList);
            hostsShortList.removeAll(avoidMsHostList);

            logger.info("Defined hosts: {} Avoid hosts: {} Available hosts: {}",
                    String.join(", ", hostsList), String.join(", ", avoidMsHostList), String.join(", ", hostsShortList));
        } catch (Exception e) {
            logger.warn("Failed to calculate next host logic", e);
        }
    }

    /**
     * Cleans up current connection state before attempting reconnection.
     * Stops host connect process, terminates links, cancels scheduled tasks,
     * notifies server resource about disconnection, and resets connection tracking.
     *
     * @param link the link that triggered reconnection
     */
    private void cleanupConnectionBeforeReconnect(Link link) {
        String lastConnectedHost = shell.getConnectedHost();
        try {
            // reset Host status track and Startup process initiating
            logger.debug("Stopping Host Connect process");
            hostConnectProcess.stop();
            closeAndTerminateLink(link);
            closeAndTerminateLink(this.link);
            setLink(null);
            cancelTasks();
            serverResource.disconnected();
            stopAndCleanupConnection();
            shell.updateConnectedHost(null);
        } catch (Exception ex) {
            logger.error("Failed to cleanup previous connection", ex);
        }
        logger.info("Lost connection to host: {}. Attempting reconnection while we still have" +
                " {} commands in progress.", lastConnectedHost, commandsInProgress.get());
    }

    /**
     * Detects reconnection storm by checking if the reconnect request is redundant.
     * This prevents processing stale reconnection requests for old links when
     * agent has already established a new connection.
     *
     * @param link the link requesting reconnection
     * @param preferredHost the preferred host to reconnect to (may be null)
     * @param requestedLink string representation of the requested link for logging
     * @param currentLink string representation of the current agent link for logging
     * @return true if reconnection storm is detected and request should be skipped, false otherwise
     */
    private boolean isReconnectStormDetected(Link link, String preferredHost, String requestedLink, String currentLink) {
        logger.debug("Calling storm guard");
        boolean reconnectForCurrentLink = link == this.link;
        boolean currentLinkTerminated = this.link != null && this.link.isTerminated();
        boolean reconnectForNewHost = this.hostname != null && this.hostname.equals(preferredHost);
        // if none of the above is true
        boolean stormDetected = ! (reconnectForCurrentLink || currentLinkTerminated || reconnectForNewHost);
        // connection storm guard
        if (stormDetected) {
            logger.warn("Reconnect requested for the connection {} but current connection is " +
                    "{} and preferred host {}, skipping", requestedLink, !currentLinkTerminated ? currentLink : currentLink + " (terminated)", preferredHost);
        }
        return stormDetected;
    }

    public void processStartupAnswer(final StartupAnswer startup, final Response response, final Link link) {
        setBackoffAlgorithm(startup);

        if (!startup.getResult()) {
            logger.error("Not allowed to connect to the server: {}", startup.getDetails());
            if (serverResource != null && !serverResource.isExitOnFailures()) {
                logger.trace("{} does not allow exit on failure, reconnecting",
                        serverResource.getClass().getSimpleName());
                logger.info("Reconnecting for {}", link);
                requestHandler.submit(() -> reconnect(link, null, false));
                return;
            }
            logger.fatal("Got unsuccessful result {} from the answer {}, details: {}",
                    startup.getResult(), startup.getClass().getSimpleName(), startup.getDetails());
            System.exit(1);
        }

        boolean processWasRunning = hostConnectProcess.stop();
        if (!processWasRunning) {
            logger.warn("Threw away a startup answer because we're reconnecting.");
            return;
        }

        handleStartupAnswer(startup, response, link);
    }

    private void handleStartupAnswer(StartupAnswer startup, Response response, Link link) {
        logger.info("Process agent startup answer, agent [id: {}, uuid: {}, name: {}] connected to the server",
                startup.getHostId(), startup.getHostUuid(), startup.getHostName());

        setId(startup.getHostId());
        // older builds do not send host uuid and names, do not set it, otherwise null pointer exception will be thrown
        String hostUuid = startup.getHostUuid();
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(hostUuid)) {
            setUuid(hostUuid);
        }
        String hostName = startup.getHostName();
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(hostName)) {
            setName(hostUuid);
        }
        pingInterval = startup.getPingInterval() * 1000L; // change to ms.

        // Process agent.host.status.check.delay.sec parameter from Management Server
        Integer agentHostStatusCheckDelaySec = startup.getAgentHostStatusCheckDelaySec();
        if (agentHostStatusCheckDelaySec != null) {
            logger.info("Received agent.host.status.check.delay.sec={} from Management Server", agentHostStatusCheckDelaySec);
            try {
                shell.setPersistentProperty(null, AgentProperties.AGENT_HOST_STATUS_CHECK_DELAY_SEC.getName(), agentHostStatusCheckDelaySec.toString());
                hostConnectProcess.updateHostStatusCheckDelay(agentHostStatusCheckDelaySec);
                logger.info("Updated agent.host.status.check.delay.sec to {} seconds", agentHostStatusCheckDelaySec);
            } catch (Exception e) {
                logger.warn("Failed to persist agent.host.status.check.delay.sec parameter from StartupAnswer", e);
            }
        }

        updateLastPingResponseTime();
        scheduleWatch(link, response, pingInterval, pingInterval);

        outRequestHandler.setKeepAliveTime(2 * pingInterval, TimeUnit.MILLISECONDS);

        logger.info("Startup Response Received: agent [id: {}, uuid: {}, name: {}]",
                startup.getHostId(), startup.getHostUuid(), startup.getHostName());
    }

    private void setBackoffAlgorithm(StartupAnswer startup) {
        try {
            logger.info("Updating backoff delay algorithm implementation, defined in {}", startup.getClass().getSimpleName());
            shell.setBackoffAlgorithm(BackoffFactory.create(startup.getParams()));
            logger.info("Created {} delay algorithm implementation", shell.getBackoffAlgorithm().getClass()
                    .getSimpleName());
        } catch (Exception e) {
            logger.warn("Failed to create backoff with provided settings {}", startup.getParams(), e);
        }
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
                        // ensures request is logged only once per method call
                        if (!requestLogged)
                        {
                            final String requestMsg = request.toString();
                            if (requestMsg != null) {
                                logger.debug("Request:{}",requestMsg);
                            }
                            requestLogged = true;
                        }
                        logger.debug("Processing command: {}", cmd);
                    }

                    if (cmd instanceof CronCommand) {
                        final CronCommand watch = (CronCommand) cmd;
                        scheduleWatch(link, request, watch.getInterval() * 1000L, watch.getInterval() * 1000L);
                        answer = new Answer(cmd, true, null);
                    } else if (cmd instanceof ShutdownCommand) {
                        final ShutdownCommand shutdown = (ShutdownCommand) cmd;
                        logger.debug("Received shutdownCommand, due to: {}", shutdown.getReason());
                        cancelTasks();
                        if (shutdown.isRemoveHost()) {
                            cleanupAgentZoneProperties();
                        }
                        reconnectAllowed = false;
                        answer = new Answer(cmd, true, null);
                    } else if (cmd instanceof ReadyCommand && ((ReadyCommand) cmd).getDetails() != null) {
                        ReadyCommand readyCmd = (ReadyCommand) cmd;
                        logger.debug("Not ready to connect to mgt server: {}", readyCmd.getDetails());
                        if (serverResource != null && !serverResource.isExitOnFailures()) {
                            logger.trace("{} does not allow exit on failure, reconnecting",
                                    serverResource.getClass().getSimpleName());
                            logger.info("Reconnecting for {}", link);
                            requestHandler.submit(() -> reconnect(link, null, false));
                            return;
                        }
                        logger.fatal("Got unsuccessful from the answer {}, details: {}",
                                readyCmd.getClass().getSimpleName(), readyCmd.getDetails());
                        System.exit(3);
                        return;
                    } else if (cmd instanceof MaintainCommand) {
                        logger.debug("Received maintainCommand, do not cancel current tasks");
                        answer = new MaintainAnswer((MaintainCommand) cmd);
                    } else if (cmd instanceof AgentControlCommand) {
                        answer = null;
                        for (final IAgentControlListener listener : controlListeners) {
                            answer = listener.processControlRequest(request, (AgentControlCommand) cmd);
                            if (answer != null) {
                                break;
                            }
                        }

                        if (answer == null) {
                            logger.warn("No handler found to process command: {}", cmd);
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
                            logger.debug("Response: unsupported command {}", cmd);
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
                } catch (final Exception e) {
                    logger.warn("Unable to send response {}: {} ", response, e.getMessage());
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

    private void processManagementServerList(final List<String> msList, final List<String> avoidMsList, final String lbAlgorithm, final Long lbCheckInterval, final boolean triggerHostLB) {
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
        if (triggerHostLB) {
            logger.info("Triggering the preferred host checker task now");
            ScheduledExecutorService hostLbExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("HostLB-Executor"));
            hostLbExecutor.schedule(new PreferredHostCheckerTask(), 0, TimeUnit.MILLISECONDS);
            hostLbExecutor.shutdown();
        }
        scheduleHostLBCheckerTask(lbAlgorithm, shell.getLbCheckerInterval(lbCheckInterval));
    }

    private Answer setupManagementServerList(final SetupMSListCommand cmd) {
        processManagementServerList(cmd.getMsList(), cmd.getAvoidMsList(), cmd.getLbAlgorithm(), cmd.getLbCheckInterval(), cmd.getTriggerHostLb());
        return new SetupMSListAnswer(true);
    }

    private Answer migrateAgentToOtherMS(final MigrateAgentConnectionCommand cmd) {
        try {
            if (CollectionUtils.isNotEmpty(cmd.getMsList())) {
                processManagementServerList(cmd.getMsList(), cmd.getAvoidMsList(), cmd.getLbAlgorithm(), cmd.getLbCheckInterval(), false);
            }
            ScheduledExecutorService migrateAgentConnectionService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("MigrateAgentConnection-Job"));
            migrateAgentConnectionService.schedule(() -> {
                migrateAgentConnection(cmd.getAvoidMsList());
            }, 3, TimeUnit.SECONDS);
            migrateAgentConnectionService.shutdown();
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

        String preferredMSHost = null;
        for (String msHost : msHostsList) {
            try (final Socket socket = new Socket()) {
                logger.debug("Connecting to {}:{} with timeout {} ms", msHost, shell.getPort(), HOST_SOCKET_CONNECT_TIMEOUT_MS);
                socket.connect(new InetSocketAddress(msHost, shell.getPort()), HOST_SOCKET_CONNECT_TIMEOUT_MS);
                logger.debug("Connected to {}:{} with timeout {} ms", msHost, shell.getPort(), HOST_SOCKET_CONNECT_TIMEOUT_MS);
                preferredMSHost = msHost;
                break;
            } catch (final IOException e) {
                throw new CloudRuntimeException(String.format("Management server host: %s:%s is not reachable, to migrate connection", msHost, shell.getPort()));
            }
        }

        if (preferredMSHost == null) {
            throw new CloudRuntimeException(String.format("Management server host(s) %s are not reachable, to migrate connection", msHostsList));
        }

        logger.debug("Management server host {} is found to be reachable, trying to reconnect", preferredMSHost);
        shell.resetHostCounter();
        shell.setAvoidHosts(avoidMsList);
        shell.setConnectionTransfer(true);

        String preferredMSHostFinal = preferredMSHost;
        logger.info("Reconnecting to {}", link);
        requestHandler.submit(() -> reconnect(link, preferredMSHostFinal, true));
    }

    public void processResponse(final Response response, final Link link) {
        final Answer answer = response.getAnswer();
        logger.debug("Received response: {}", response);
        if (answer instanceof StartupAnswer) {
            processStartupAnswer((StartupAnswer) answer, response, link);
        } else if (answer instanceof AgentControlAnswer) {
            // Notice, we are doing callback while holding a lock!
            for (final IAgentControlListener listener : controlListeners) {
                listener.processControlResponse(response, (AgentControlAnswer) answer);
            }
        } else if (answer instanceof PingAnswer) {
            processPingAnswer(link, (PingAnswer) answer);
        } else {
            ServerAttache attachment = (ServerAttache) link.attachment();
            if (attachment != null) {
                attachment.processAnswers(response.getSequence(), response);
            } else {
                logger.trace("No attachments in the link, nothing to process");
            }
            updateLastPingResponseTime();
        }
    }

    private void processPingAnswer(final Link link, final PingAnswer answer) {
        if ((answer.isSendStartup()) && reconnectAllowed) {
            logger.info("Management server requested startup command to reinitialize the agent");
            hostConnectProcess.scheduleConnectProcess(link, false);
        } else {
            serverResource.processPingAnswer(answer);
        }
        shell.setAvoidHosts(answer.getAvoidMsList());
    }

    public void processReadyCommand(final Command cmd) {
        final ReadyCommand ready = (ReadyCommand) cmd;
        // Set human readable sizes;
        Boolean humanReadable = ready.getEnableHumanReadableSizes();
        if (humanReadable != null) {
            NumbersUtil.enableHumanReadableSizes = humanReadable;
        }

        logger.info("Processing agent ready command, agent id = {}, uuid = {}, name = {}", ready.getHostId(), ready.getHostUuid(), ready.getHostName());
        if (ready.getHostId() != null) {
            setId(ready.getHostId());
            setUuid(ready.getHostUuid());
            setName(ready.getHostName());
        }

        verifyAgentArch(ready.getArch());
        processManagementServerList(ready.getMsHostList(), ready.getAvoidMsHostList(), ready.getLbAlgorithm(), ready.getLbCheckInterval(), false);

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
            logger.debug("Sending ping: {}", request);

            try {
                task.getLink().send(request.toBytes());
                //if i can send pingcommand out, means the link is ok
                updateLastPingResponseTime();
            } catch (final ClosedChannelException e) {
                logger.warn("Unable to send request to {} due to '{}', request: {}",
                        task.getLink(), e.getMessage(), request);
            }

        } else if (obj instanceof Request) {
            final Request req = (Request) obj;
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

                logger.debug("Watch Sent: {}", response);
                try {
                    task.getLink().send(response.toBytes());
                } catch (final ClosedChannelException e) {
                    logger.warn("Unable to send response: {}", response);
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
        final Request request = new Request(getId(), -1, new Command[]{cmd}, true, false);
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
        final Request request = new Request(getId(), -1, new Command[]{cmd}, true, false);
        request.setSequence(getNextSequence());
        postRequest(request);
    }

    private void postRequest(final Request request) throws AgentControlChannelException {
        if (link != null) {
            try {
                link.send(request.toBytes());
            } catch (final ClosedChannelException e) {
                logger.warn("Unable to post agent control request: {}", request, e);
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

    public class AgentRequestHandler extends Task {
        public AgentRequestHandler(final Task.Type type, final Link link, final Request req) {
            super(type, link, req);
        }

        @Override
        protected void doTask(final Task task) {
            final Request req = (Request) get();
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
        public void doTask(final Task task) {
            if (task.getType() == Task.Type.CONNECT) {
                shell.getBackoffAlgorithm().reset();
                setLink(task.getLink());
                Link link = task.getLink();
                link.attach(new ServerAttache(link));
                hostConnectProcess.scheduleConnectProcess(task.getLink(), shell.isConnectionTransfer());
                shell.setConnectionTransfer(false);
            } else if (task.getType() == Task.Type.DATA) {
                Request request;
                try {
                    request = Request.parse(task.getData());
                    if (request instanceof Response) {
                        //It's for pinganswer etc, should be processed immediately.
                        processResponse((Response) request, task.getLink());
                    } else {
                        //put the requests from mgt server into another thread pool, as the request may take a longer
                        // time to finish. Don't block the NIO main thread pool
                        requestHandler.submit(new AgentRequestHandler(getType(), getLink(), request));
                    }
                } catch (final ClassNotFoundException e) {
                    logger.error("Unable to find this request ");
                } catch (final Exception e) {
                    logger.error("Error parsing task", e);
                }
            } else if (task.getType() == Task.Type.DISCONNECT) {
                try {
                    // an issue has been found if reconnect immediately after disconnecting.
                    // wait 5 seconds before reconnecting
                    logger.debug("Wait for 5 secs before reconnecting, disconnect task - {}", task.getLink());
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
                shell.setConnectionTransfer(false);
                logger.debug("Executing disconnect task - {} and reconnecting", task.getLink());
                requestHandler.submit(() -> reconnect(task.getLink(), null, false));
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
            logger.debug("Running preferred host checker task");
            try {
                String preferredMSHost = Optional.ofNullable(shell.getHosts())
                        .filter(ArrayUtils::isNotEmpty)
                        .map(hosts -> hosts[0])
                        .orElse(null);

                if (StringUtils.isBlank(preferredMSHost)) {
                    logger.debug("Preferred host is not defined, skip task");
                    return;
                }

                List<String> avoidHosts = Optional.ofNullable(shell.getAvoidHosts()).orElseGet(List::of);
                if (avoidHosts.contains(preferredMSHost)) {
                    logger.debug("Preferred host {} is in avoid list {}, skip task",
                            preferredMSHost, String.join(", ", avoidHosts));
                    return;
                }
                String connectedHost = shell.getConnectedHost();
                String connectedIp = NetUtils.resolveToIp(connectedHost);
                String preferredIp = NetUtils.resolveToIp(preferredMSHost);
                if (Objects.equals(connectedIp, preferredIp) || Objects.equals(connectedHost, preferredMSHost)) {
                    logger.debug(String.format("Already connected to the preferred host (%s), skip task",
                            preferredMSHost));
                    return;
                }
                if (link == null) {
                    logger.debug("Agent is not connected to any host now, skip task");
                    return;
                }

                if (commandsInProgress.get() > 0) {
                    logger.debug("There are still {} commands in progress, skip task", commandsInProgress.get());
                    return;
                }

                if (hostConnectProcess.isInProgress()) {
                    logger.warn("Unable to reconnect because there is already running Host connect process on {}, skip task",
                            shell.getConnectedHost());
                    return;
                }

                logger.debug("Running preferred host checker task, preferred host: {} connected host: {}",
                        preferredMSHost, connectedHost);

                boolean isHostUp;
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(preferredMSHost, shell.getPort()), HOST_SOCKET_CONNECT_TIMEOUT_MS);
                    isHostUp = true;
                } catch (IOException e) {
                    isHostUp = false;
                    logger.debug("Host: {} is not reachable", preferredMSHost);
                }
                if (isHostUp && link != null && commandsInProgress.get() == 0 && isLockAvailable()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Preferred host {} is found to be reachable, trying to reconnect", preferredMSHost);
                    }
                    shell.resetHostCounter();
                    logger.info("Reconnecting to {}", link);
                    reconnect(link, preferredMSHost, false);
                }
            } catch (Throwable t) {
                logger.error("Error caught while attempting to connect to preferred host", t);
            }
        }

        /**
         * Sends {@link AgentConnectStatusCommand} to the currently connected Management Server
         * and checks whether {@link AgentConnectStatusAnswer#isLockAvailable()}.
         *
         * @return true if lock available
         */
        private boolean isLockAvailable() {
            boolean lockAvailable = true;
            ServerAttache attache = (ServerAttache) link.attachment();
            if (attache != null && attache.getLink() != null) {
                AgentConnectStatusCommand command = setupAgentConnectStatusCommand(new AgentConnectStatusCommand());
                Command[] commands = new Command[]{command};
                try {
                    AgentConnectStatusAnswer answer = attache.send(getId(), commands, AgentConnectStatusAnswer.class,
                            DEFAULT_ASYNC_COMMAND_TIMEOUT_SEC);
                    lockAvailable = Optional.ofNullable(answer)
                            .map(AgentConnectStatusAnswer::isLockAvailable)
                            .orElse(Boolean.FALSE);
                    if (!lockAvailable) {
                        logger.info(String.format("There is lock and Host status is %s, will retry later",
                                answer.getHostStatus()));
                    }
                } catch (IOException | RuntimeException e) {
                    String commandName = commands[0].getClass().getSimpleName();
                    logger.error(String.format("Failed to retrieve %s, will retry later", commandName), e);
                }
            }
            return lockAvailable;
        }
    }

    @Override
    public int getMaxConcurrentNewConnectionsCount() {
        return shell.getWorkers();
    }

    @Override
    public int getNewConnectionsCount() {
        logger.trace("Get new connections count called");
        return 0;
    }

    @Override
    public void registerNewConnection(InetSocketAddress address) {
        logger.trace("Register new connection to {}", address);
    }

    @Override
    public void unregisterNewConnection(InetSocketAddress address) {
        logger.trace("Unregister new connection to {}", address);
    }

    public String getPersistentProperty(String name) {
        return shell.getPersistentProperty(null, name);
    }
}
