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
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.ConfigurationException;

import com.cloud.utils.NumbersUtil;
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
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CronCommand;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.ShutdownCommand;
import com.cloud.agent.api.StartupAnswer;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.transport.Request;
import com.cloud.agent.transport.Response;
import com.cloud.exception.AgentControlChannelException;
import com.cloud.host.Host;
import com.cloud.resource.ServerResource;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.StringUtils;
import com.cloud.utils.backoff.BackoffAlgorithm;
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
import com.google.common.base.Strings;

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
public class Agent implements HandlerFactory, IAgentControl {
    private static final Logger s_logger = Logger.getLogger(Agent.class.getName());

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

    List<IAgentControlListener> _controlListeners = new ArrayList<IAgentControlListener>();

    IAgentShell _shell;
    NioConnection _connection;
    ServerResource _resource;
    Link _link;
    Long _id;

    Timer _timer = new Timer("Agent Timer");
    Timer certTimer;
    Timer hostLBTimer;

    List<WatchTask> _watchList = new ArrayList<WatchTask>();
    long _sequence = 0;
    long _lastPingResponseTime = 0;
    long _pingInterval = 0;
    AtomicInteger _inProgress = new AtomicInteger();

    StartupTask _startup = null;
    long _startupWaitDefault = 180000;
    long _startupWait = _startupWaitDefault;
    boolean _reconnectAllowed = true;
    //For time sentitive task, e.g. PingTask
    ThreadPoolExecutor _ugentTaskPool;
    ExecutorService _executor;

    Thread _shutdownThread = new ShutdownThread(this);

    private String _keystoreSetupPath;
    private String _keystoreCertImportPath;

    // for simulator use only
    public Agent(final IAgentShell shell) {
        _shell = shell;
        _link = null;

        _connection = new NioClient("Agent", _shell.getNextHost(), _shell.getPort(), _shell.getWorkers(), this);

        Runtime.getRuntime().addShutdownHook(_shutdownThread);

        _ugentTaskPool =
                new ThreadPoolExecutor(shell.getPingRetries(), 2 * shell.getPingRetries(), 10, TimeUnit.MINUTES, new SynchronousQueue<Runnable>(), new NamedThreadFactory(
                        "UgentTask"));

        _executor =
                new ThreadPoolExecutor(_shell.getWorkers(), 5 * _shell.getWorkers(), 1, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory(
                        "agentRequest-Handler"));
    }

    public Agent(final IAgentShell shell, final int localAgentId, final ServerResource resource) throws ConfigurationException {
        _shell = shell;
        _resource = resource;
        _link = null;

        resource.setAgentControl(this);

        final String value = _shell.getPersistentProperty(getResourceName(), "id");
        _id = value != null ? Long.parseLong(value) : null;
        s_logger.info("id is " + (_id != null ? _id : ""));

        final Map<String, Object> params = PropertiesUtil.toMap(_shell.getProperties());

        // merge with properties from command line to let resource access command line parameters
        for (final Map.Entry<String, Object> cmdLineProp : _shell.getCmdLineProperties().entrySet()) {
            params.put(cmdLineProp.getKey(), cmdLineProp.getValue());
        }

        if (!_resource.configure(getResourceName(), params)) {
            throw new ConfigurationException("Unable to configure " + _resource.getName());
        }

        final String host = _shell.getNextHost();
        _connection = new NioClient("Agent", host, _shell.getPort(), _shell.getWorkers(), this);

        // ((NioClient)_connection).setBindAddress(_shell.getPrivateIp());

        s_logger.debug("Adding shutdown hook");
        Runtime.getRuntime().addShutdownHook(_shutdownThread);

        _ugentTaskPool =
                new ThreadPoolExecutor(shell.getPingRetries(), 2 * shell.getPingRetries(), 10, TimeUnit.MINUTES, new SynchronousQueue<Runnable>(), new NamedThreadFactory(
                        "UgentTask"));

        _executor =
                new ThreadPoolExecutor(_shell.getWorkers(), 5 * _shell.getWorkers(), 1, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory(
                        "agentRequest-Handler"));

        s_logger.info("Agent [id = " + (_id != null ? _id : "new") + " : type = " + getResourceName() + " : zone = " + _shell.getZone() + " : pod = " + _shell.getPod() +
                " : workers = " + _shell.getWorkers() + " : host = " + host + " : port = " + _shell.getPort());
    }

    public String getVersion() {
        return _shell.getVersion();
    }

    public String getResourceGuid() {
        final String guid = _shell.getGuid();
        return guid + "-" + getResourceName();
    }

    public String getZone() {
        return _shell.getZone();
    }

    public String getPod() {
        return _shell.getPod();
    }

    protected void setLink(final Link link) {
        _link = link;
    }

    public ServerResource getResource() {
        return _resource;
    }

    public BackoffAlgorithm getBackoffAlgorithm() {
        return _shell.getBackoffAlgorithm();
    }

    public String getResourceName() {
        return _resource.getClass().getSimpleName();
    }

    /**
     * In case of a software based agent restart, this method
     * can help to perform explicit garbage collection of any old
     * agent instances and its inner objects.
     */
    private void scavengeOldAgentObjects() {
        _executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000L);
                } catch (final InterruptedException ignored) {
                } finally {
                    System.gc();
                }
            }
        });
    }

    public void start() {
        if (!_resource.start()) {
            s_logger.error("Unable to start the resource: " + _resource.getName());
            throw new CloudRuntimeException("Unable to start the resource: " + _resource.getName());
        }

        _keystoreSetupPath = Script.findScript("scripts/util/", KeyStoreUtils.KS_SETUP_SCRIPT);
        if (_keystoreSetupPath == null) {
            throw new CloudRuntimeException(String.format("Unable to find the '%s' script", KeyStoreUtils.KS_SETUP_SCRIPT));
        }

        _keystoreCertImportPath = Script.findScript("scripts/util/", KeyStoreUtils.KS_IMPORT_SCRIPT);
        if (_keystoreCertImportPath == null) {
            throw new CloudRuntimeException(String.format("Unable to find the '%s' script", KeyStoreUtils.KS_IMPORT_SCRIPT));
        }

        try {
            _connection.start();
        } catch (final NioConnectionException e) {
            s_logger.warn("NIO Connection Exception  " + e);
            s_logger.info("Attempted to connect to the server, but received an unexpected exception, trying again...");
        }
        while (!_connection.isStartup()) {
            final String host = _shell.getNextHost();
            _shell.getBackoffAlgorithm().waitBeforeRetry();
            _connection = new NioClient("Agent", host, _shell.getPort(), _shell.getWorkers(), this);
            s_logger.info("Connecting to host:" + host);
            try {
                _connection.start();
            } catch (final NioConnectionException e) {
                _connection.stop();
                try {
                    _connection.cleanUp();
                } catch (final IOException ex) {
                    s_logger.warn("Fail to clean up old connection. " + ex);
                }
                s_logger.info("Attempted to connect to the server, but received an unexpected exception, trying again...", e);
            }
        }
        _shell.updateConnectedHost();
        scavengeOldAgentObjects();
    }

    public void stop(final String reason, final String detail) {
        s_logger.info("Stopping the agent: Reason = " + reason + (detail != null ? ": Detail = " + detail : ""));
        _reconnectAllowed = false;
        if (_connection != null) {
            final ShutdownCommand cmd = new ShutdownCommand(reason, detail);
            try {
                if (_link != null) {
                    final Request req = new Request(_id != null ? _id : -1, -1, cmd, false);
                    _link.send(req.toBytes());
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
            _connection.stop();
            _connection = null;
            _link = null;
        }

        if (_resource != null) {
            _resource.stop();
            _resource = null;
        }

        if (_startup != null) {
            _startup = null;
        }

        if (_ugentTaskPool != null) {
            _ugentTaskPool.shutdownNow();
            _ugentTaskPool = null;
        }

        if (_executor != null) {
            _executor.shutdown();
            _executor = null;
        }

        if (_timer != null) {
            _timer.cancel();
            _timer = null;
        }

        if (hostLBTimer != null) {
            hostLBTimer.cancel();
            hostLBTimer = null;
        }

        if (certTimer != null) {
            certTimer.cancel();
            certTimer = null;
        }
    }

    public Long getId() {
        return _id;
    }

    public void setId(final Long id) {
        s_logger.info("Set agent id " + id);
        _id = id;
        _shell.setPersistentProperty(getResourceName(), "id", Long.toString(id));
    }

    private synchronized void scheduleServicesRestartTask() {
        if (certTimer != null) {
            certTimer.cancel();
            certTimer.purge();
        }
        certTimer = new Timer("Certificate Renewal Timer");
        certTimer.schedule(new PostCertificateRenewalTask(this), 5000L);
    }

    private synchronized void scheduleHostLBCheckerTask(final long checkInterval) {
        if (hostLBTimer != null) {
            hostLBTimer.cancel();
        }
        if (checkInterval > 0L) {
            s_logger.info("Scheduling preferred host timer task with host.lb.interval=" + checkInterval + "ms");
            hostLBTimer = new Timer("Host LB Timer");
            hostLBTimer.scheduleAtFixedRate(new PreferredHostCheckerTask(), checkInterval, checkInterval);
        }
    }

    public void scheduleWatch(final Link link, final Request request, final long delay, final long period) {
        synchronized (_watchList) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Adding a watch list");
            }
            final WatchTask task = new WatchTask(link, request, this);
            _timer.schedule(task, 0, period);
            _watchList.add(task);
        }
    }

    protected void cancelTasks() {
        synchronized (_watchList) {
            for (final WatchTask task : _watchList) {
                task.cancel();
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Clearing watch list: " + _watchList.size());
            }
            _watchList.clear();
        }
    }

    /**
     * Cleanup agent zone properties.
     *
     * Unset zone, cluster and pod values so that host is not added back
     * when service is restarted. This will be set to proper values
     * when host is added back
     */
    protected void cleanupAgentZoneProperties() {
        _shell.setPersistentProperty(null, "zone", "");
        _shell.setPersistentProperty(null, "cluster", "");
        _shell.setPersistentProperty(null, "pod", "");
    }

    public synchronized void lockStartupTask(final Link link) {
        _startup = new StartupTask(link);
        _timer.schedule(_startup, _startupWait);
    }

    public void sendStartup(final Link link) {
        final StartupCommand[] startup = _resource.initialize();
        if (startup != null) {
            final String msHostList = _shell.getPersistentProperty(null, "host");
            final Command[] commands = new Command[startup.length];
            for (int i = 0; i < startup.length; i++) {
                setupStartupCommand(startup[i]);
                startup[i].setMSHostList(msHostList);
                commands[i] = startup[i];
            }
            final Request request = new Request(_id != null ? _id : -1, -1, commands, false, false);
            request.setSequence(getNextSequence());

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Sending Startup: " + request.toString());
            }
            lockStartupTask(link);
            try {
                link.send(request.toBytes());
            } catch (final ClosedChannelException e) {
                s_logger.warn("Unable to send reques: " + request.toString());
            }
        }
    }

    protected void setupStartupCommand(final StartupCommand startup) {
        InetAddress addr;
        try {
            addr = InetAddress.getLocalHost();
        } catch (final UnknownHostException e) {
            s_logger.warn("unknow host? ", e);
            throw new CloudRuntimeException("Cannot get local IP address");
        }

        final Script command = new Script("hostname", 500, s_logger);
        final OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
        final String result = command.execute(parser);
        final String hostname = result == null ? parser.getLine() : addr.toString();

        startup.setId(getId());
        if (startup.getName() == null) {
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

    protected void reconnect(final Link link) {
        if (!_reconnectAllowed) {
            return;
        }
        synchronized (this) {
            if (_startup != null) {
                _startup.cancel();
                _startup = null;
            }
        }

        if (link != null) {
            link.close();
            link.terminated();
        }

        setLink(null);
        cancelTasks();

        _resource.disconnected();

        s_logger.info("Lost connection to host: " + _shell.getConnectedHost() + ". Attempting reconnection while we still have " + _inProgress.get() + " commands in progress.");

        _connection.stop();

        try {
            _connection.cleanUp();
        } catch (final IOException e) {
            s_logger.warn("Fail to clean up old connection. " + e);
        }

        while (_connection.isStartup()) {
            _shell.getBackoffAlgorithm().waitBeforeRetry();
        }

        do {
            final String host = _shell.getNextHost();
            _connection = new NioClient("Agent", host, _shell.getPort(), _shell.getWorkers(), this);
            s_logger.info("Reconnecting to host:" + host);
            try {
                _connection.start();
            } catch (final NioConnectionException e) {
                s_logger.info("Attempted to re-connect to the server, but received an unexpected exception, trying again...", e);
                _connection.stop();
                try {
                    _connection.cleanUp();
                } catch (final IOException ex) {
                    s_logger.warn("Fail to clean up old connection. " + ex);
                }
            }
            _shell.getBackoffAlgorithm().waitBeforeRetry();
        } while (!_connection.isStartup());
        _shell.updateConnectedHost();
        s_logger.info("Connected to the host: " + _shell.getConnectedHost());
    }

    public void processStartupAnswer(final Answer answer, final Response response, final Link link) {
        boolean cancelled = false;
        synchronized (this) {
            if (_startup != null) {
                _startup.cancel();
                _startup = null;
            } else {
                cancelled = true;
            }
        }
        final StartupAnswer startup = (StartupAnswer)answer;
        if (!startup.getResult()) {
            s_logger.error("Not allowed to connect to the server: " + answer.getDetails());
            System.exit(1);
        }
        if (cancelled) {
            s_logger.warn("Threw away a startup answer because we're reconnecting.");
            return;
        }

        s_logger.info("Proccess agent startup answer, agent id = " + startup.getHostId());

        setId(startup.getHostId());
        _pingInterval = (long)startup.getPingInterval() * 1000; // change to ms.

        setLastPingResponseTime();
        scheduleWatch(link, response, _pingInterval, _pingInterval);

        _ugentTaskPool.setKeepAliveTime(2 * _pingInterval, TimeUnit.MILLISECONDS);

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
                        scheduleWatch(link, request, (long)watch.getInterval() * 1000, watch.getInterval() * 1000);
                        answer = new Answer(cmd, true, null);
                    } else if (cmd instanceof ShutdownCommand) {
                        final ShutdownCommand shutdown = (ShutdownCommand)cmd;
                        s_logger.debug("Received shutdownCommand, due to: " + shutdown.getReason());
                        cancelTasks();
                        if (shutdown.isRemoveHost()) {
                            cleanupAgentZoneProperties();
                        }
                        _reconnectAllowed = false;
                        answer = new Answer(cmd, true, null);
                    } else if (cmd instanceof ReadyCommand && ((ReadyCommand)cmd).getDetails() != null) {
                        s_logger.debug("Not ready to connect to mgt server: " + ((ReadyCommand)cmd).getDetails());
                        System.exit(1);
                        return;
                    } else if (cmd instanceof MaintainCommand) {
                        s_logger.debug("Received maintainCommand, do not cancel current tasks");
                        answer = new MaintainAnswer((MaintainCommand)cmd);
                    } else if (cmd instanceof AgentControlCommand) {
                        answer = null;
                        synchronized (_controlListeners) {
                            for (final IAgentControlListener listener : _controlListeners) {
                                answer = listener.processControlRequest(request, (AgentControlCommand)cmd);
                                if (answer != null) {
                                    break;
                                }
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
                        if (Host.Type.Routing.equals(_resource.getType())) {
                            scheduleServicesRestartTask();
                        }
                    } else if (cmd instanceof SetupMSListCommand) {
                        answer = setupManagementServerList((SetupMSListCommand) cmd);
                    } else {
                        if (cmd instanceof ReadyCommand) {
                            processReadyCommand(cmd);
                        }
                        _inProgress.incrementAndGet();
                        try {
                            answer = _resource.executeRequest(cmd);
                        } finally {
                            _inProgress.decrementAndGet();
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

        String storedPassword = _shell.getPersistentProperty(null, KeyStoreUtils.KS_PASSPHRASE_PROPERTY);
        if (Strings.isNullOrEmpty(storedPassword)) {
            storedPassword = keyStorePassword;
            _shell.setPersistentProperty(null, KeyStoreUtils.KS_PASSPHRASE_PROPERTY, storedPassword);
        }

        Script script = new Script(_keystoreSetupPath, 300000, s_logger);
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

        Script script = new Script(_keystoreCertImportPath, 300000, s_logger);
        script.add(agentFile.getAbsolutePath());
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
        if (CollectionUtils.isNotEmpty(msList) && !Strings.isNullOrEmpty(lbAlgorithm)) {
            try {
                final String newMSHosts = String.format("%s%s%s", StringUtils.toCSVList(msList), IAgentShell.hostLbAlgorithmSeparator, lbAlgorithm);
                _shell.setPersistentProperty(null, "host", newMSHosts);
                _shell.setHosts(newMSHosts);
                _shell.resetHostCounter();
                s_logger.info("Processed new management server list: " + newMSHosts);
            } catch (final Exception e) {
                throw new CloudRuntimeException("Could not persist received management servers list", e);
            }
        }
        if ("shuffle".equals(lbAlgorithm)) {
            scheduleHostLBCheckerTask(0);
        } else {
            scheduleHostLBCheckerTask(_shell.getLbCheckerInterval(lbCheckInterval));
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
            synchronized (_controlListeners) {
                for (final IAgentControlListener listener : _controlListeners) {
                    listener.processControlResponse(response, (AgentControlAnswer)answer);
                }
            }
        } else {
            setLastPingResponseTime();
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
            if (System.currentTimeMillis() - _lastPingResponseTime > _pingInterval * _shell.getPingRetries()) {
                s_logger.error("Ping Interval has gone past " + _pingInterval * _shell.getPingRetries() + ". Won't reconnect to mgt server, as connection is still alive");
                return;
            }

            final PingCommand ping = _resource.getCurrentStatus(getId());
            final Request request = new Request(_id, -1, ping, false);
            request.setSequence(getNextSequence());
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Sending ping: " + request.toString());
            }

            try {
                task.getLink().send(request.toBytes());
                //if i can send pingcommand out, means the link is ok
                setLastPingResponseTime();
            } catch (final ClosedChannelException e) {
                s_logger.warn("Unable to send request: " + request.toString());
            }

        } else if (obj instanceof Request) {
            final Request req = (Request)obj;
            final Command command = req.getCommand();
            if (command.getContextParam("logid") != null) {
                MDC.put("logcontextid", command.getContextParam("logid"));
            }
            Answer answer = null;
            _inProgress.incrementAndGet();
            try {
                answer = _resource.executeRequest(command);
            } finally {
                _inProgress.decrementAndGet();
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

    public synchronized void setLastPingResponseTime() {
        _lastPingResponseTime = System.currentTimeMillis();
    }

    protected synchronized long getNextSequence() {
        return _sequence++;
    }

    @Override
    public void registerControlListener(final IAgentControlListener listener) {
        synchronized (_controlListeners) {
            _controlListeners.add(listener);
        }
    }

    @Override
    public void unregisterControlListener(final IAgentControlListener listener) {
        synchronized (_controlListeners) {
            _controlListeners.remove(listener);
        }
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
                    s_logger.warn("sendRequest is interrupted, exit waiting");
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
        if (_link != null) {
            try {
                _link.send(request.toBytes());
            } catch (final ClosedChannelException e) {
                s_logger.warn("Unable to post agent control reques: " + request.toString());
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
                    _ugentTaskPool.submit(new ServerHandler(Task.Type.OTHER, _link, _request));
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
        protected volatile boolean cancelled = false;

        public StartupTask(final Link link) {
            s_logger.debug("Startup task created");
            _link = link;
        }

        @Override
        public synchronized boolean cancel() {
            // TimerTask.cancel may fail depends on the calling context
            if (!cancelled) {
                cancelled = true;
                _startupWait = _startupWaitDefault;
                s_logger.debug("Startup task cancelled");
                return super.cancel();
            }
            return true;
        }

        @Override
        protected synchronized void runInContext() {
            if (!cancelled) {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("The startup command is now cancelled");
                }
                cancelled = true;
                _startup = null;
                _startupWait = _startupWaitDefault * 2;
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
                _shell.getBackoffAlgorithm().reset();
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
                        _executor.submit(new AgentRequestHandler(getType(), getLink(), request));
                    }
                } catch (final ClassNotFoundException e) {
                    s_logger.error("Unable to find this request ");
                } catch (final Exception e) {
                    s_logger.error("Error parsing task", e);
                }
            } else if (task.getType() == Task.Type.DISCONNECT) {
                reconnect(task.getLink());
                return;
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
                    if (_inProgress.get() == 0) {
                        s_logger.debug("Running post certificate renewal task to restart services.");

                        // Let the resource perform any post certificate renewal cleanups
                        _resource.executeRequest(new PostCertificateRenewalCommand());

                        IAgentShell shell = agent._shell;
                        ServerResource resource = agent._resource.getClass().newInstance();

                        // Stop current agent
                        agent.cancelTasks();
                        agent._reconnectAllowed = false;
                        Runtime.getRuntime().removeShutdownHook(agent._shutdownThread);
                        agent.stop(ShutdownCommand.Requested, "Restarting due to new X509 certificates");

                        // Nullify references for GC
                        agent._shell = null;
                        agent._watchList = null;
                        agent._shutdownThread = null;
                        agent._controlListeners = null;
                        agent = null;

                        // Start a new agent instance
                        shell.launchNewAgent(resource);
                        return;
                    }
                    if (s_logger.isTraceEnabled()) {
                        s_logger.debug("Other tasks are in progress, will retry post certificate renewal command after few seconds");
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
                final String[] msList = _shell.getHosts();
                if (msList == null || msList.length < 1) {
                    return;
                }
                final String preferredHost  = msList[0];
                final String connectedHost = _shell.getConnectedHost();
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("Running preferred host checker task, connected host=" + connectedHost + ", preferred host=" + preferredHost);
                }
                if (preferredHost != null && !preferredHost.equals(connectedHost) && _link != null) {
                    boolean isHostUp = true;
                    try (final Socket socket = new Socket()) {
                        socket.connect(new InetSocketAddress(preferredHost, _shell.getPort()), 5000);
                    } catch (final IOException e) {
                        isHostUp = false;
                        if (s_logger.isTraceEnabled()) {
                            s_logger.trace("Host: " + preferredHost + " is not reachable");
                        }
                    }
                    if (isHostUp && _link != null && _inProgress.get() == 0) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Preferred host " + preferredHost + " is found to be reachable, trying to reconnect");
                        }
                        _shell.resetHostCounter();
                        reconnect(_link);
                    }
                }
            } catch (Throwable t) {
                s_logger.error("Error caught while attempting to connect to preferred host", t);
            }
        }

    }

}
