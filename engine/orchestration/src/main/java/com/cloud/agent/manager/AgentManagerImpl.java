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
package com.cloud.agent.manager;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.agent.lb.IndirectAgentLB;
import org.apache.cloudstack.ca.CAManager;
import org.apache.cloudstack.command.ReconcileCommandService;
import org.apache.cloudstack.command.ReconcileCommandUtils;
import org.apache.cloudstack.command.ReconcileCommandVO;
import org.apache.cloudstack.command.dao.ReconcileCommandDao;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobExecutionContext;
import org.apache.cloudstack.maintenance.ManagementServerMaintenanceListener;
import org.apache.cloudstack.maintenance.ManagementServerMaintenanceManager;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.management.ManagementServerHost;
import org.apache.cloudstack.outofbandmanagement.dao.OutOfBandManagementDao;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.ThreadContext;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.StartupCommandProcessor;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingAnswer;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.SetHostParamsCommand;
import com.cloud.agent.api.ShutdownCommand;
import com.cloud.agent.api.StartupAnswer;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupProxyCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StartupSecondaryStorageCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.api.UnsupportedAnswer;
import com.cloud.agent.transport.Request;
import com.cloud.agent.transport.Response;
import com.cloud.alert.AlertManager;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.configuration.Config;
import com.cloud.configuration.ManagementServiceConfiguration;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.UnsupportedVersionException;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.Status.Event;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.org.Cluster;
import com.cloud.resource.Discoverer;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.resource.ServerResource;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.HypervisorVersionChangedException;
import com.cloud.utils.exception.NioConnectionException;
import com.cloud.utils.exception.TaskExecutionException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.nio.HandlerFactory;
import com.cloud.utils.nio.Link;
import com.cloud.utils.nio.NioServer;
import com.cloud.utils.nio.Task;
import com.cloud.utils.time.InaccurateClock;

/**
 * Implementation of the Agent Manager. This class controls the connection to the agents.
 **/
public class AgentManagerImpl extends ManagerBase implements AgentManager, HandlerFactory, ManagementServerMaintenanceListener, Configurable {

    /**
     * _agents is a ConcurrentHashMap, but it is used from within a synchronized block. This will be reported by findbugs as JLM_JSR166_UTILCONCURRENT_MONITORENTER. Maybe a
     * ConcurrentHashMap is not the right thing to use here, but i'm not sure so i leave it alone.
     */
    protected ConcurrentHashMap<Long, AgentAttache> _agents = new ConcurrentHashMap<>(10007);
    protected List<Pair<Integer, Listener>> _hostMonitors = new ArrayList<>(17);
    protected List<Pair<Integer, Listener>> _cmdMonitors = new ArrayList<>(17);
    protected List<Pair<Integer, StartupCommandProcessor>> _creationMonitors = new ArrayList<>(17);
    protected List<Long> _loadingAgents = new ArrayList<>();
    protected Map<String, Integer> _commandTimeouts = new HashMap<>();
    private int _monitorId = 0;

    @Inject
    protected CAManager caService;
    @Inject
    protected EntityManager _entityMgr;

    protected NioServer _connection;
    @Inject
    protected HostDao _hostDao = null;
    @Inject
    private ManagementServerHostDao _mshostDao;
    @Inject
    protected OutOfBandManagementDao outOfBandManagementDao;
    @Inject
    protected DataCenterDao _dcDao = null;
    @Inject
    protected HostPodDao _podDao = null;
    @Inject
    protected ConfigurationDao _configDao = null;
    @Inject
    protected ClusterDao _clusterDao = null;

    @Inject
    protected HighAvailabilityManager _haMgr = null;
    @Inject
    protected AlertManager _alertMgr = null;
    @Inject
    protected ReconcileCommandService reconcileCommandService;
    @Inject
    ReconcileCommandDao reconcileCommandDao;

    @Inject
    protected HypervisorGuruManager _hvGuruMgr;

    @Inject
    protected IndirectAgentLB indirectAgentLB;

    @Inject
    private ManagementServerMaintenanceManager managementServerMaintenanceManager;

    protected int _retry = 2;

    protected long _nodeId = -1;

    protected ExecutorService _executor;
    protected ThreadPoolExecutor _connectExecutor;
    protected ScheduledExecutorService _directAgentExecutor;
    protected ScheduledExecutorService _cronJobExecutor;
    protected ScheduledExecutorService _monitorExecutor;

    private int _directAgentThreadCap;

    private List<String> lastAgents = null;

    protected StateMachine2<Status, Status.Event, Host> _statusStateMachine = Status.getStateMachine();
    private final ConcurrentHashMap<Long, Long> _pingMap = new ConcurrentHashMap<>(10007);
    private int maxConcurrentNewAgentConnections;
    private final ConcurrentHashMap<String, Long> newAgentConnections = new ConcurrentHashMap<>();
    protected ScheduledExecutorService newAgentConnectionsMonitor;

    private boolean _reconcileCommandsEnabled = false;
    private Integer _reconcileCommandInterval;

    @Inject
    ResourceManager _resourceMgr;
    @Inject
    ManagementServiceConfiguration mgmtServiceConf;

    protected final ConfigKey<Integer> Workers = new ConfigKey<>("Advanced", Integer.class, "workers", "5",
            "Number of worker threads handling remote agent connections.", false);
    protected final ConfigKey<Integer> Port = new ConfigKey<>("Advanced", Integer.class, "port", "8250", "Port to listen on for remote (indirect) agent connections.", false);
    protected final ConfigKey<Integer> RemoteAgentSslHandshakeTimeout = new ConfigKey<>("Advanced",
            Integer.class, "agent.ssl.handshake.timeout", "30",
            "Seconds after which SSL handshake times out during remote (indirect) agent connections.", false);
    protected final ConfigKey<Integer> RemoteAgentMaxConcurrentNewConnections = new ConfigKey<>("Advanced",
            Integer.class, "agent.max.concurrent.new.connections", "0",
            "Number of maximum concurrent new connections server allows for remote (indirect) agents. " +
                    "If set to zero (default value) then no limit will be enforced on concurrent new connections",
            false);
    protected final ConfigKey<Integer> RemoteAgentNewConnectionsMonitorInterval = new ConfigKey<>("Advanced", Integer.class, "agent.connections.monitor.interval", "1800",
            "Time in seconds to monitor the new agent connections and cleanup the expired connections.", false);
    protected final ConfigKey<Integer> AlertWait = new ConfigKey<>("Advanced", Integer.class, "alert.wait", "1800",
            "Seconds to wait before alerting on a disconnected agent", true);
    protected final ConfigKey<Integer> DirectAgentLoadSize = new ConfigKey<>("Advanced", Integer.class, "direct.agent.load.size", "16",
            "The number of direct agents to load each time", false);
    protected final ConfigKey<Integer> DirectAgentPoolSize = new ConfigKey<>("Advanced", Integer.class, "direct.agent.pool.size", "500",
            "Default size for DirectAgentPool", false);
    protected final ConfigKey<Float> DirectAgentThreadCap = new ConfigKey<>("Advanced", Float.class, "direct.agent.thread.cap", "1",
            "Percentage (as a value between 0 and 1) of direct.agent.pool.size to be used as upper thread cap for a single direct agent to process requests", false);
    protected final ConfigKey<Boolean> CheckTxnBeforeSending = new ConfigKey<>("Developer", Boolean.class, "check.txn.before.sending.agent.commands", "false",
            "This parameter allows developers to enable a check to see if a transaction wraps commands that are sent to the resource.  This is not to be enabled on production systems.", true);

    public static final List<Host.Type> HOST_DOWN_ALERT_UNSUPPORTED_HOST_TYPES = Arrays.asList(
            Host.Type.SecondaryStorage,
            Host.Type.ConsoleProxy
    );

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {

        logger.info("Ping Timeout is {}.", mgmtServiceConf.getPingTimeout());

        _nodeId = ManagementServerNode.getManagementServerId();
        logger.info("Configuring AgentManagerImpl. management server node id(msid): {}.", _nodeId);

        final long lastPing = (System.currentTimeMillis() >> 10) - mgmtServiceConf.getTimeout();
        _hostDao.markHostsAsDisconnected(_nodeId, lastPing);

        registerForHostEvents(new BehindOnPingListener(), true, true, false);

        registerForHostEvents(new SetHostParamsListener(), true, true, false);

        managementServerMaintenanceManager.registerListener(this);

        final int agentTaskThreads = DirectAgentLoadSize.value();

        _executor = new ThreadPoolExecutor(agentTaskThreads, agentTaskThreads, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new NamedThreadFactory("AgentTaskPool"));

        maxConcurrentNewAgentConnections = RemoteAgentMaxConcurrentNewConnections.value();

        _connection = new NioServer("AgentManager", Port.value(), Workers.value() + 10,
                this, caService, RemoteAgentSslHandshakeTimeout.value());
        logger.info("Listening on {} with {} workers.", Port.value(), Workers.value());

        final int directAgentPoolSize = DirectAgentPoolSize.value();
        // executes all agent commands other than cron and ping
        _directAgentExecutor = new ScheduledThreadPoolExecutor(directAgentPoolSize, new NamedThreadFactory("DirectAgent"));
        // executes cron and ping agent commands
        _cronJobExecutor = new ScheduledThreadPoolExecutor(directAgentPoolSize, new NamedThreadFactory("DirectAgentCronJob"));
        logger.debug("Created DirectAgentAttache pool with size: {}.", directAgentPoolSize);
        _directAgentThreadCap = Math.round(directAgentPoolSize * DirectAgentThreadCap.value()) + 1; // add 1 to always make the value > 0

        initializeCommandTimeouts();

        _reconcileCommandsEnabled = ReconcileCommandService.ReconcileCommandsEnabled.value();
        _reconcileCommandInterval = ReconcileCommandService.ReconcileCommandsInterval.value();

        return true;
    }

    @Override
    public Task create(final Task.Type type, final Link link, final byte[] data) {
        return new AgentHandler(type, link, data);
    }

    @Override
    public int getMaxConcurrentNewConnectionsCount() {
        return maxConcurrentNewAgentConnections;
    }

    @Override
    public int getNewConnectionsCount() {
        return newAgentConnections.size();
    }

    @Override
    public void registerNewConnection(SocketAddress address) {
        logger.trace("Adding new agent connection from {}", address.toString());
        newAgentConnections.putIfAbsent(address.toString(), System.currentTimeMillis());
    }

    @Override
    public void unregisterNewConnection(SocketAddress address) {
        logger.trace("Removing new agent connection for {}", address.toString());
        newAgentConnections.remove(address.toString());
    }

    @Override
    public int registerForHostEvents(final Listener listener, final boolean connections, final boolean commands, final boolean priority) {
        synchronized (_hostMonitors) {
            _monitorId++;
            if (connections) {
                if (priority) {
                    _hostMonitors.add(0, new Pair<>(_monitorId, listener));
                } else {
                    _hostMonitors.add(new Pair<>(_monitorId, listener));
                }
            }
            if (commands) {
                if (priority) {
                    _cmdMonitors.add(0, new Pair<>(_monitorId, listener));
                } else {
                    _cmdMonitors.add(new Pair<>(_monitorId, listener));
                }
            }
            logger.debug("Registering listener {} with id {}", listener.getClass().getSimpleName(), _monitorId);
            return _monitorId;
        }
    }

    @Override
    public int registerForInitialConnects(final StartupCommandProcessor creator, final boolean priority) {
        synchronized (_hostMonitors) {
            _monitorId++;
            if (priority) {
                _creationMonitors.add(0, new Pair<>(_monitorId, creator));
            } else {
                _creationMonitors.add(new Pair<>(_monitorId, creator));
            }
            return _monitorId;
        }
    }

    @Override
    public void unregisterForHostEvents(final int id) {
        logger.debug("Deregistering {}", id);
        _hostMonitors.remove(id);
    }

    @Override
    public void onManagementServerPreparingForMaintenance() {
        logger.debug("Management server preparing for maintenance");
        if (_connection != null) {
            _connection.block();
        }
    }

    @Override
    public void onManagementServerCancelPreparingForMaintenance() {
        logger.debug("Management server cancel preparing for maintenance");
        if (_connection != null) {
            _connection.unblock();
        }
    }

    @Override
    public void onManagementServerMaintenance() {
        logger.debug("Management server maintenance enabled");
        _monitorExecutor.shutdownNow();
        newAgentConnectionsMonitor.shutdownNow();
        if (_connection != null) {
            _connection.stop();

            try {
                _connection.cleanUp();
            } catch (final IOException e) {
                logger.warn("Fail to clean up old connection", e);
            }
        }
        _connectExecutor.shutdownNow();
    }

    @Override
    public void onManagementServerCancelMaintenance() {
        logger.debug("Management server maintenance disabled");
        if (_connectExecutor.isShutdown()) {
            initConnectExecutor();
        }
        startDirectlyConnectedHosts(true);
        if (_connection != null) {
            try {
                _connection.start();
            } catch (final NioConnectionException e) {
                logger.error("Error when connecting to the NioServer!", e);
            }
        }

        if (_monitorExecutor.isShutdown()) {
            initAndScheduleMonitorExecutor();
        }
        if (newAgentConnectionsMonitor.isShutdown()) {
            initAndScheduleAgentConnectionsMonitor();
        }
    }

    private void initConnectExecutor() {
        _connectExecutor = new ThreadPoolExecutor(100, 500, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new NamedThreadFactory("AgentConnectTaskPool"));
        // allow core threads to time out even when there are no items in the queue
        _connectExecutor.allowCoreThreadTimeOut(true);
    }

    private void initAndScheduleMonitorExecutor() {
        _monitorExecutor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("AgentMonitor"));
        _monitorExecutor.scheduleWithFixedDelay(new MonitorTask(), mgmtServiceConf.getPingInterval(), mgmtServiceConf.getPingInterval(), TimeUnit.SECONDS);
    }

    private void initAndScheduleAgentConnectionsMonitor() {
        final int cleanupTimeInSecs = Wait.value();
        newAgentConnectionsMonitor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("NewAgentConnectionsMonitor"));
        newAgentConnectionsMonitor.scheduleAtFixedRate(new AgentNewConnectionsMonitorTask(), cleanupTimeInSecs, cleanupTimeInSecs, TimeUnit.SECONDS);
    }

    private AgentControlAnswer handleControlCommand(final AgentAttache attache, final AgentControlCommand cmd) {
        AgentControlAnswer answer;

        for (final Pair<Integer, Listener> listener : _cmdMonitors) {
            answer = listener.second().processControlCommand(attache.getId(), cmd);

            if (answer != null) {
                return answer;
            }
        }

        logger.warn("No handling of agent control command: {} sent from {}", cmd, attache);
        return new AgentControlAnswer(cmd);
    }

    public void handleCommands(final AgentAttache attache, final long sequence, final Command[] cmds) {
        for (final Pair<Integer, Listener> listener : _cmdMonitors) {
            final boolean processed = listener.second().processCommands(attache.getId(), sequence, cmds);
            logger.trace("SeqA {}-{}: {} by {}", attache.getId(), sequence, (processed ? "processed" : "not processed"), listener.getClass());
        }
    }

    public void notifyAnswersToMonitors(final long agentId, final long seq, final Answer[] answers) {
        for (final Pair<Integer, Listener> listener : _cmdMonitors) {
            listener.second().processAnswers(agentId, seq, answers);
        }
    }

    public AgentAttache findAttache(final long hostId) {
        AgentAttache attache;
        synchronized (_agents) {
            attache = _agents.get(hostId);
        }
        return attache;
    }

    @Override
    public Answer sendTo(final Long dcId, final HypervisorType type, final Command cmd) {
        final List<ClusterVO> clusters = _clusterDao.listByDcHyType(dcId, type.toString());
        int retry = 0;
        for (final ClusterVO cluster : clusters) {
            final List<HostVO> hosts = _resourceMgr.listAllUpAndEnabledHosts(Host.Type.Routing, cluster.getId(), null, dcId);
            for (final HostVO host : hosts) {
                retry++;
                if (retry > _retry) {
                    return null;
                }
                Answer answer = null;
                try {
                    final long targetHostId = _hvGuruMgr.getGuruProcessedCommandTargetHost(host.getId(), cmd, host.getHypervisorType());
                    answer = easySend(targetHostId, cmd);
                } catch (final Exception e) {
                    String errorMsg = String.format("Error sending command %s to host %s, due to %s", cmd.getClass().getName(),
                            host, e.getLocalizedMessage());
                    logger.error(errorMsg);
                    logger.debug(errorMsg, e);
                }
                if (answer != null) {
                    return answer;
                }
            }
        }
        return null;
    }

    @Override
    public Answer send(final Long hostId, final Command cmd) throws AgentUnavailableException, OperationTimedoutException {
        final Commands cmds = new Commands(Command.OnError.Stop);
        cmds.addCommand(cmd);
        send(hostId, cmds, cmd.getWait());
        final Answer[] answers = cmds.getAnswers();
        if (answers != null) {
            if (answers[0] instanceof UnsupportedAnswer) {
                logger.warn("Unsupported Command: {}", answers[0].getDetails());
            }
            return answers[0];
        }

        return null;
    }

    @DB
    protected boolean noDbTxn() {
        final TransactionLegacy txn = TransactionLegacy.currentTxn();
        return !txn.dbTxnStarted();
    }

    private static void tagCommand(final Command cmd) {
        final AsyncJobExecutionContext context = AsyncJobExecutionContext.getCurrent();
        if (context != null && context.getJob() != null) {
            final AsyncJob job = context.getJob();

            if (job.getRelated() != null && !job.getRelated().isEmpty()) {
                cmd.setContextParam("job", "job-" + job.getRelated() + "/" + "job-" + job.getId());
            } else {
                cmd.setContextParam("job", "job-" + job.getId());
            }
        }
        String logcontextid = ThreadContext.get("logcontextid");
        if (StringUtils.isNotEmpty(logcontextid)) {
            cmd.setContextParam("logid", logcontextid);
        }
    }

    /**
     * @param commands object container of commands
     * @return array of commands
     */
    private Command[] checkForCommandsAndTag(final Commands commands) {
        final Command[] cmds = commands.toCommands();

        assert cmds.length > 0 : "Ask yourself this about a hundred times.  Why am I  sending zero length commands?";

        setEmptyAnswers(commands, cmds);

        for (final Command cmd : cmds) {
            tagCommand(cmd);
        }
        return cmds;
    }

    /**
     * @param commands object container of commands
     * @param cmds array of commands
     */
    private void setEmptyAnswers(final Commands commands, final Command[] cmds) {
        if (cmds.length == 0) {
            commands.setAnswers(new Answer[0]);
        }
    }

    protected int getTimeout(final Commands commands, int timeout) {
        int result;
        if (timeout > 0) {
            result = timeout;
        } else {
            result = Wait.value();
        }

        int granularTimeout = getTimeoutFromGranularWaitTime(commands);
        return (granularTimeout > 0) ? granularTimeout : result;
    }

    protected int getTimeoutFromGranularWaitTime(final Commands commands) {
        int maxWait = 0;
        if (MapUtils.isNotEmpty(_commandTimeouts)) {
            for (final Command cmd : commands) {
                String simpleCommandName = cmd.getClass().getSimpleName();
                Integer commandTimeout = _commandTimeouts.get(simpleCommandName);
                if (commandTimeout != null && commandTimeout > maxWait) {
                    maxWait = commandTimeout;
                }
            }
        }

        return maxWait;
    }

    private void initializeCommandTimeouts() {
        String commandWaits = GranularWaitTimeForCommands.value().trim();
        if (StringUtils.isNotEmpty(commandWaits)) {
            _commandTimeouts = getCommandTimeoutsMap(commandWaits);
            logger.info("Timeouts for management server internal commands successfully initialized from global setting commands.timeout: {}", _commandTimeouts);
        }
    }

    private Map<String, Integer> getCommandTimeoutsMap(String commandWaits) {
        String[] commandPairs = commandWaits.split(",");
        Map<String, Integer> commandTimeouts = new HashMap<>();

        for (String commandPair : commandPairs) {
            String[] parts = commandPair.trim().split("=");
            if (parts.length == 2) {
                try {
                    String commandName = parts[0].trim();
                    int commandTimeout = Integer.parseInt(parts[1].trim());
                    commandTimeouts.put(commandName, commandTimeout);
                } catch (NumberFormatException e) {
                    logger.error("Initialising the timeouts using commands.timeout: {} for management server internal commands failed with error {}", commandPair, e.getMessage());
                }
            } else {
                logger.error("Error initialising the timeouts for management server internal commands. Invalid format in commands.timeout: {}", commandPair);
            }
        }
        return commandTimeouts;
    }

    @Override
    public Answer[] send(final Long hostId, final Commands commands, int timeout) throws AgentUnavailableException, OperationTimedoutException {
        assert hostId != null : "Who's not checking the agent id before sending?  ... (finger wagging)";
        if (hostId == null) {
            throw new AgentUnavailableException(-1);
        }

        int wait = getTimeout(commands, timeout);
        logger.debug("Wait time setting on {} is {} seconds", commands, wait);
        for (Command cmd : commands) {
            String simpleCommandName = cmd.getClass().getSimpleName();
            Integer commandTimeout = _commandTimeouts.get(simpleCommandName);
            if (commandTimeout != null) {
                cmd.setWait(wait);
            }
        }

        if (CheckTxnBeforeSending.value()) {
            if (!noDbTxn()) {
                throw new CloudRuntimeException("We do not allow transactions to be wrapped around commands sent to be executed on remote agents.  "
                        + "We cannot predict how long it takes a command to complete.  " + "The transaction may be rolled back because the connection took too long.");
            }
        } else {
            assert noDbTxn() : "I know, I know.  Why are we so strict as to not allow txn across an agent call?  ...  Why are we so cruel ... Why are we such a dictator .... Too bad... Sorry...but NO AGENT COMMANDS WRAPPED WITHIN DB TRANSACTIONS!";
        }

        final Command[] cmds = checkForCommandsAndTag(commands);

        //check what agent is returned.
        final AgentAttache agent = getAttache(hostId);
        if (agent == null || agent.isClosed()) {
            throw new AgentUnavailableException("agent not logged into this management server", hostId);
        }

        final Request req = new Request(hostId, agent.getName(), _nodeId, cmds, commands.stopOnError(), true);
        req.setSequence(agent.getNextSequence());

        reconcileCommandService.persistReconcileCommands(hostId, req.getSequence(), cmds);

        final Answer[] answers = agent.send(req, wait);

        reconcileCommandService.processAnswers(req.getSequence(), cmds, answers);

        notifyAnswersToMonitors(hostId, req.getSequence(), answers);
        commands.setAnswers(answers);
        return answers;
    }

    protected Status investigate(final AgentAttache agent) {
        final Long hostId = agent.getId();
        final HostVO host = _hostDao.findById(hostId);
        if (host == null || host.getType() == null || host.getType().isVirtual()) {
            return Status.Alert;
        }
        logger.debug("Checking if agent ({}) is alive", host);
        final Answer answer = easySend(hostId, new CheckHealthCommand());
        if (answer != null && answer.getResult()) {
            final Status status = Status.Up;
            logger.debug("Agent ({}) responded to checkHealthCommand, reporting that agent is {}", host, status);
            return status;
        }
        return _haMgr.investigate(hostId);
    }

    protected AgentAttache getAttache(final Long hostId) throws AgentUnavailableException {
        if (hostId == null) {
            return null;
        }
        final AgentAttache agent = findAttache(hostId);
        if (agent == null) {
            logger.debug("Unable to find agent for {}", hostId);
            throw new AgentUnavailableException("Unable to find agent ", hostId);
        }

        return agent;
    }

    @Override
    public long send(final Long hostId, final Commands commands, final Listener listener) throws AgentUnavailableException {
        final AgentAttache agent = getAttache(hostId);
        if (agent.isClosed()) {
            throw new AgentUnavailableException(String.format(
                    "Agent [id: %d, name: %s] is closed",
                    agent.getId(), agent.getName()), agent.getId());
        }

        final Command[] cmds = checkForCommandsAndTag(commands);

        final Request req = new Request(hostId, agent.getName(), _nodeId, cmds, commands.stopOnError(), true);
        req.setSequence(agent.getNextSequence());

        agent.send(req, listener);
        return req.getSequence();
    }

    public void removeAgent(final AgentAttache attache, final Status nextState) {
        if (attache == null) {
            return;
        }
        final long hostId = attache.getId();
        logger.debug("Remove Agent : {}", attache);
        AgentAttache removed;
        boolean conflict = false;
        synchronized (_agents) {
            removed = _agents.remove(hostId);
            if (removed != null && removed != attache) {
                conflict = true;
                _agents.put(hostId, removed);
                removed = attache;
            }
        }
        if (conflict) {
            logger.debug("Agent for host {} is created when it is being disconnected", attache);
        }
        if (removed != null) {
            removed.disconnect(nextState);
        }

        for (final Pair<Integer, Listener> monitor : _hostMonitors) {
            logger.debug("Sending Disconnect to listener: {}", monitor.second().getClass().getName());
            monitor.second().processDisconnect(hostId, attache.getUuid(), attache.getName(), nextState);
        }
    }

    @Override
    public void notifyMonitorsOfNewlyAddedHost(long hostId) {
        for (final Pair<Integer, Listener> monitor : _hostMonitors) {
            logger.debug("Sending host added to listener: {}", monitor.second().getClass().getSimpleName());

            monitor.second().processHostAdded(hostId);
        }
    }

    protected AgentAttache notifyMonitorsOfConnection(final AgentAttache attache, final StartupCommand[] cmds, final boolean forRebalance) throws ConnectionException {
        final long hostId = attache.getId();
        final HostVO host = _hostDao.findById(hostId);
        for (final Pair<Integer, Listener> monitor : _hostMonitors) {
            logger.debug("Sending Connect to listener: {}, for rebalance: {}", monitor.second().getClass().getSimpleName(), forRebalance);
            for (StartupCommand cmd : cmds) {
                try {
                    logger.debug("process connection to issue: {} for host: {}, forRebalance: {}", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(cmd, "id", "type", "msHostList", "connectionTransferred"), hostId, forRebalance);
                    monitor.second().processConnect(host, cmd, forRebalance);
                } catch (final ConnectionException ce) {
                    if (ce.isSetupError()) {
                        logger.warn("Monitor {} says there is an error in the connect process for {} due to {}", monitor.second().getClass().getSimpleName(), hostId, ce.getMessage());
                        handleDisconnectWithoutInvestigation(attache, Event.AgentDisconnected, true, true);
                        throw ce;
                    } else {
                        logger.info("Monitor {} says not to continue the connect process for {} due to {}", monitor.second().getClass().getSimpleName(), hostId, ce.getMessage());
                        handleDisconnectWithoutInvestigation(attache, Event.ShutdownRequested, true, true);
                        return attache;
                    }
                } catch (final HypervisorVersionChangedException hvce) {
                    handleDisconnectWithoutInvestigation(attache, Event.ShutdownRequested, true, true);
                    throw new CloudRuntimeException("Unable to connect " + attache.getId(), hvce);
                } catch (final Exception e) {
                    logger.error("Monitor {} says there is an error in the connect process for {} due to {}", monitor.second().getClass().getSimpleName(), hostId, e.getMessage(), e);
                    handleDisconnectWithoutInvestigation(attache, Event.AgentDisconnected, true, true);
                    throw new CloudRuntimeException("Unable to connect " + attache.getId(), e);
                }
            }
        }

        final ReadyCommand ready = new ReadyCommand(host, NumbersUtil.enableHumanReadableSizes);
        ready.setWait(ReadyCommandWait.value());
        final Answer answer = easySend(hostId, ready);
        if (answer == null || !answer.getResult()) {
            // this is tricky part for secondary storage
            // make it as disconnected, wait for secondary storage VM to be up
            // return the attache instead of null, even it is disconnectede
            handleDisconnectWithoutInvestigation(attache, Event.AgentDisconnected, true, true);
        }
        if (answer instanceof ReadyAnswer) {
            ReadyAnswer readyAnswer = (ReadyAnswer)answer;
            Map<String, String> detailsMap = readyAnswer.getDetailsMap();
            if (detailsMap != null) {
                String uefiEnabled = detailsMap.get(Host.HOST_UEFI_ENABLE);
                String virtv2vVersion = detailsMap.get(Host.HOST_VIRTV2V_VERSION);
                String ovftoolVersion = detailsMap.get(Host.HOST_OVFTOOL_VERSION);
                logger.debug("Got HOST_UEFI_ENABLE [{}] for host [{}]:", uefiEnabled, host);
                if (ObjectUtils.anyNotNull(uefiEnabled, virtv2vVersion, ovftoolVersion)) {
                    _hostDao.loadDetails(host);
                    boolean updateNeeded = false;
                    if (StringUtils.isNotBlank(uefiEnabled) && !uefiEnabled.equals(host.getDetails().get(Host.HOST_UEFI_ENABLE))) {
                        host.getDetails().put(Host.HOST_UEFI_ENABLE, uefiEnabled);
                        updateNeeded = true;
                    }
                    if (StringUtils.isNotBlank(virtv2vVersion) && !virtv2vVersion.equals(host.getDetails().get(Host.HOST_VIRTV2V_VERSION))) {
                        host.getDetails().put(Host.HOST_VIRTV2V_VERSION, virtv2vVersion);
                        updateNeeded = true;
                    }
                    if (StringUtils.isNotBlank(ovftoolVersion) && !ovftoolVersion.equals(host.getDetails().get(Host.HOST_OVFTOOL_VERSION))) {
                        host.getDetails().put(Host.HOST_OVFTOOL_VERSION, ovftoolVersion);
                        updateNeeded = true;
                    }
                    if (updateNeeded) {
                        _hostDao.saveDetails(host);
                    }
                }
            }
        }

        agentStatusTransitTo(host, Event.Ready, _nodeId);
        attache.ready();
        return attache;
    }

    @Override
    public boolean start() {
        ManagementServerHostVO msHost = _mshostDao.findByMsid(_nodeId);
        if (msHost != null && (ManagementServerHost.State.Maintenance.equals(msHost.getState()) || ManagementServerHost.State.PreparingForMaintenance.equals(msHost.getState()))) {
            _monitorExecutor.shutdownNow();
            newAgentConnectionsMonitor.shutdownNow();
            return true;
        }

        initConnectExecutor();
        startDirectlyConnectedHosts(false);

        if (_connection != null) {
            try {
                _connection.start();
            } catch (final NioConnectionException e) {
                logger.error("Error when connecting to the NioServer!", e);
            }
        }

        initAndScheduleMonitorExecutor();
        initAndScheduleAgentConnectionsMonitor();
        return true;
    }

    public void startDirectlyConnectedHosts(final boolean forRebalance) {
        final List<HostVO> hosts = _resourceMgr.findDirectlyConnectedHosts();
        for (final HostVO host : hosts) {
            loadDirectlyConnectedHost(host, forRebalance);
        }
    }

    private ServerResource loadResourcesWithoutHypervisor(final HostVO host) {
        final String resourceName = host.getResource();
        ServerResource resource = null;
        try {
            final Class<?> clazz = Class.forName(resourceName);
            final Constructor<?> constructor = clazz.getConstructor();
            resource = (ServerResource)constructor.newInstance();
        } catch (final ClassNotFoundException e) {
            logger.warn("Unable to find class {}", host.getResource(), e);
        } catch (final InstantiationException e) {
            logger.warn("Unable to instantiate class {}", host.getResource(), e);
        } catch (final IllegalAccessException e) {
            logger.warn("Illegal access {}", host.getResource(), e);
        } catch (final SecurityException e) {
            logger.warn("Security error on {}", host.getResource(), e);
        } catch (final NoSuchMethodException e) {
            logger.warn("NoSuchMethodException error on {}", host.getResource(), e);
        } catch (final IllegalArgumentException e) {
            logger.warn("IllegalArgumentException error on {}", host.getResource(), e);
        } catch (final InvocationTargetException e) {
            logger.warn("InvocationTargetException error on {}", host.getResource(), e);
        }

        if (resource != null) {
            _hostDao.loadDetails(host);

            final HashMap<String, Object> params = new HashMap<>(host.getDetails().size() + 5);
            params.putAll(host.getDetails());

            params.put("guid", host.getGuid());
            params.put("zone", Long.toString(host.getDataCenterId()));
            if (host.getPodId() != null) {
                params.put("pod", Long.toString(host.getPodId()));
            }
            if (host.getClusterId() != null) {
                params.put("cluster", Long.toString(host.getClusterId()));
                String guid;
                final ClusterVO cluster = _clusterDao.findById(host.getClusterId());
                if (cluster.getGuid() == null) {
                    guid = host.getDetail("pool");
                } else {
                    guid = cluster.getGuid();
                }
                if (guid != null && !guid.isEmpty()) {
                    params.put("pool", guid);
                }
            }

            params.put("ipaddress", host.getPrivateIpAddress());
            params.put("secondary.storage.vm", "false");

            try {
                resource.configure(host.getName(), params);
            } catch (final ConfigurationException e) {
                logger.warn("Unable to configure resource due to {}", e.getMessage());
                return null;
            }

            if (!resource.start()) {
                logger.warn("Unable to start the resource");
                return null;
            }
        }
        return resource;
    }

    @Override
    public void rescan() {
    }

    protected boolean loadDirectlyConnectedHost(final HostVO host, final boolean forRebalance) {
        return loadDirectlyConnectedHost(host, forRebalance, false);
    }

    protected boolean loadDirectlyConnectedHost(final HostVO host, final boolean forRebalance, final boolean isTransferredConnection) {
        boolean initialized = false;
        ServerResource resource;
        try {
            // load the respective discoverer
            final Discoverer discoverer = _resourceMgr.getMatchingDiscover(host.getHypervisorType());
            if (discoverer == null) {
                logger.info("Could not to find a Discoverer to load the resource: {} for hypervisor type: {}", host, host.getHypervisorType());
                resource = loadResourcesWithoutHypervisor(host);
            } else {
                resource = discoverer.reloadResource(host);
            }

            if (resource == null) {
                logger.warn("Unable to load the resource: {}", host);
                return false;
            }

            initialized = true;
        } finally {
            if (!initialized) {
                if (host != null) {
                    agentStatusTransitTo(host, Event.AgentDisconnected, _nodeId);
                }
            }
        }

        if (forRebalance) {
            tapLoadingAgents(host.getId(), TapAgentsAction.Add);
            final Host h = _resourceMgr.createHostAndAgent(host.getId(), resource, host.getDetails(), false, null, true, isTransferredConnection);
            tapLoadingAgents(host.getId(), TapAgentsAction.Del);

            return h != null;
        } else {
            _executor.execute(new SimulateStartTask(host.getId(), host.getUuid(), host.getName(), resource, host.getDetails()));
            return true;
        }
    }

    protected AgentAttache createAttacheForDirectConnect(final Host host, final ServerResource resource) {
        logger.debug("create DirectAgentAttache for {}", host);
        final DirectAgentAttache attache = new DirectAgentAttache(this, host.getId(), host.getUuid(), host.getName(), host.getHypervisorType(), resource, host.isInMaintenanceStates());

        AgentAttache old;
        synchronized (_agents) {
            old = _agents.put(host.getId(), attache);
        }
        if (old != null) {
            old.disconnect(Status.Removed);
        }

        return attache;
    }

    @Override
    public boolean stop() {

        if (_connection != null) {
            _connection.stop();
        }

        logger.info("Disconnecting agents: {}", _agents.size());
        synchronized (_agents) {
            for (final AgentAttache agent : _agents.values()) {
                final HostVO host = _hostDao.findById(agent.getId());
                if (host == null) {
                    logger.debug("Cannot find host {}", agent);
                } else {
                    if (!agent.forForward()) {
                        agentStatusTransitTo(host, Event.ManagementServerDown, _nodeId);
                    }
                }
            }
        }

        _connectExecutor.shutdownNow();
        _monitorExecutor.shutdownNow();
        newAgentConnectionsMonitor.shutdownNow();
        return true;
    }

    protected Status getNextStatusOnDisconnection(Host host, final Status.Event event) {
        final Status currentStatus = host.getStatus();
        Status nextStatus;
        if (currentStatus == Status.Down || currentStatus == Status.Alert || currentStatus == Status.Removed) {
            logger.debug("Host {} is already {}", host, currentStatus);
            nextStatus = currentStatus;
        } else {
            try {
                nextStatus = currentStatus.getNextStatus(event);
            } catch (final NoTransitionException e) {
                final String err = String.format("Cannot find next status for %s as current status is %s for agent %s", event, currentStatus, host);
                logger.debug(err);
                throw new CloudRuntimeException(err);
            }
            logger.debug("The next status of agent {} is {}, current status is {}", host, nextStatus, currentStatus);
        }
        return nextStatus;
    }

    protected boolean handleDisconnectWithoutInvestigation(final AgentAttache attache, final Status.Event event, final boolean transitState, final boolean removeAgent) {
        final long hostId = attache.getId();
        final HostVO host = _hostDao.findById(hostId);
        boolean result = false;
        GlobalLock joinLock = getHostJoinLock(hostId);
        try {
            if (!joinLock.lock(60)) {
                logger.debug("Unable to acquire lock on host {} to process agent disconnection", Objects.toString(host, String.valueOf(hostId)));
                return result;
            }

            logger.debug("Acquired lock on host {}, to process agent disconnection", Objects.toString(host, String.valueOf(hostId)));
            disconnectHostAgent(attache, event, host, transitState, joinLock);
            result = true;
        } finally {
            joinLock.releaseRef();
        }

        return result;
    }

    private void disconnectHostAgent(final AgentAttache attache, final Status.Event event, final HostVO host, final boolean transitState, final GlobalLock joinLock) {
        try {
            logger.info("Host {} is disconnecting with event {}", attache, event);
            final long hostId = attache.getId();
            Status nextStatus;
            if (host == null) {
                logger.warn("Can't find host with {} ({})", hostId, attache);
                nextStatus = Status.Removed;
            } else {
                nextStatus = getNextStatusOnDisconnection(host, event);
                caService.purgeHostCertificate(host);
            }
            logger.debug("Deregistering link for {} with state {}", attache, nextStatus);

            removeAgent(attache, nextStatus);

            if (host != null && transitState) {
                // update the state for host in DB as per the event
                disconnectAgent(host, event, _nodeId);
            }
        } finally {
            joinLock.unlock();
        }
    }

    protected boolean handleDisconnectWithInvestigation(final AgentAttache attache, Status.Event event) {
        final long hostId = attache.getId();
        HostVO host = _hostDao.findById(hostId);

        if (host != null) {
            Status nextStatus = null;
            try {
                nextStatus = host.getStatus().getNextStatus(event);
            } catch (final NoTransitionException ne) {
                /*
                 * Agent may be currently in status of Down, Alert, Removed, namely there is no next status for some events. Why this can happen? Ask God not me. I hate there was
                 * no piece of comment for code handling race condition. God knew what race condition the code dealt with!
                 */
                logger.debug("Caught exception while getting agent's next status", ne);
            }

            if (nextStatus == Status.Alert) {
                /* OK, we are going to the bad status, let's see what happened */
                logger.info("Investigating why host {} has disconnected with event", host, event);

                Status determinedState = investigate(attache);
                // if state cannot be determined do nothing and bail out
                if (determinedState == null) {
                    if ((System.currentTimeMillis() >> 10) - host.getLastPinged() > AlertWait.value()) {
                        logger.warn("Agent {} state cannot be determined for more than {} ({}) seconds, will go to Alert state",
                                host, AlertWait, AlertWait.value());
                        determinedState = Status.Alert;
                    } else {
                        logger.warn("Agent {} state cannot be determined, do nothing", host);
                        return false;
                    }
                }

                final Status currentStatus = host.getStatus();
                logger.info("The agent from host {} state determined is {}", host, determinedState);

                if (determinedState == Status.Down) {
                    final String message = String.format("Host %s is down. Starting HA on the VMs", host);
                    logger.error(message);
                    if (Status.Down.equals(host.getStatus())) {
                        logger.debug(String.format("Skipping sending alert for %s as it already in %s state",
                                host, host.getStatus()));
                    } else if (!HOST_DOWN_ALERT_UNSUPPORTED_HOST_TYPES.contains(host.getType())) {
                        _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, host.getDataCenterId(), host.getPodId(), "Host down, " + host.getId(), message);
                    }
                    event = Status.Event.HostDown;
                } else if (determinedState == Status.Up) {
                    /* Got ping response from host, bring it back */
                    logger.info("Agent is determined to be up and running");
                    agentStatusTransitTo(host, Status.Event.Ping, _nodeId);
                    return false;
                } else if (determinedState == Status.Disconnected) {
                    logger.warn("Agent is disconnected but the host is still up: {} state: {}", host, host.getResourceState());
                    if (currentStatus == Status.Disconnected ||
                            (currentStatus == Status.Up && host.getResourceState() == ResourceState.PrepareForMaintenance)) {
                        if ((System.currentTimeMillis() >> 10) - host.getLastPinged() > AlertWait.value()) {
                            logger.warn("Host {} has been disconnected past the wait time it should be disconnected.", host);
                            event = Status.Event.WaitedTooLong;
                        } else {
                            logger.debug("Host {} has been determined to be disconnected but it hasn't passed the wait time yet.", host);
                            return false;
                        }
                    } else if (currentStatus == Status.Up) {
                        final DataCenterVO dcVO = _dcDao.findById(host.getDataCenterId());
                        final HostPodVO podVO = _podDao.findById(host.getPodId());
                        final String hostDesc = "name: " + host.getName() + " (id:" + host.getUuid() + "), availability zone: " + dcVO.getName() + ", pod: " + podVO.getName();
                        if (host.getType() != Host.Type.SecondaryStorage && host.getType() != Host.Type.ConsoleProxy) {
                            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, host.getDataCenterId(), host.getPodId(), "Host disconnected, " + hostDesc,
                                    "If the agent for host [" + hostDesc + "] is not restarted within " + AlertWait + " seconds, host will go to Alert state");
                        }
                        event = Status.Event.AgentDisconnected;
                    }
                } else {
                    // if we end up here we are in alert state, send an alert
                    final DataCenterVO dcVO = _dcDao.findById(host.getDataCenterId());
                    final HostPodVO podVO = _podDao.findById(host.getPodId());
                    final String podName = podVO != null ? podVO.getName() : "NO POD";
                    final String hostDesc = String.format("%s, availability zone: %s, pod: %s", host, dcVO, podName);
                    _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST,
                            host.getDataCenterId(), host.getPodId(),
                            String.format("Host in ALERT state, %s", hostDesc),
                            String.format("In availability zone %s, host is in alert state: %s", dcVO, host));
                }
            } else {
                logger.debug("The next status of agent {} is not Alert, no need to investigate what happened", host);
            }
        }
        handleDisconnectWithoutInvestigation(attache, event, true, true);
        host = _hostDao.findById(hostId); // Maybe the host magically reappeared?
        if (host != null && host.getStatus() == Status.Down) {
            _haMgr.scheduleRestartForVmsOnHost(host, true, HighAvailabilityManager.ReasonType.HostDown);
            reconcileCommandService.updateReconcileCommandToInterruptedByHostId(hostId);
        }
        return true;
    }

    protected class DisconnectTask extends ManagedContextRunnable {
        AgentAttache _attache;
        Status.Event _event;
        boolean _investigate;

        DisconnectTask(final AgentAttache attache, final Status.Event event, final boolean investigate) {
            _attache = attache;
            _event = event;
            _investigate = investigate;
        }

        @Override
        protected void runInContext() {
            try {
                if (_investigate) {
                    handleDisconnectWithInvestigation(_attache, _event);
                } else {
                    handleDisconnectWithoutInvestigation(_attache, _event, true, false);
                }
            } catch (final Exception e) {
                logger.error("Exception caught while handling disconnect: ", e);
            }
        }
    }

    @Override
    public Answer easySend(final Long hostId, final Command cmd) {
        try {
            final Host h = _hostDao.findById(hostId);
            if (h == null || h.getRemoved() != null) {
                logger.debug("Host with id {} doesn't exist", hostId);
                return null;
            }
            final Status status = h.getStatus();
            if (!status.equals(Status.Up) && !status.equals(Status.Connecting)) {
                logger.debug("Can not send command {} due to Host {} not being up", cmd, h);
                return null;
            }
            final Answer answer = send(hostId, cmd);
            if (answer == null) {
                logger.warn("send returns null answer");
                return null;
            }

            if (logger.isDebugEnabled() && answer.getDetails() != null) {
                logger.debug("Details from executing {}: {}", cmd.getClass(), answer.getDetails());
            }

            return answer;

        } catch (final AgentUnavailableException e) {
            logger.warn(e.getMessage());
            return null;
        } catch (final OperationTimedoutException e) {
            logger.warn("Operation timed out: {}", e.getMessage());
            return null;
        } catch (final Exception e) {
            logger.warn("Exception while sending", e);
            return null;
        }
    }

    @Override
    public Answer[] send(final Long hostId, final Commands cmds) throws AgentUnavailableException, OperationTimedoutException {
        int wait = 0;
        if (cmds.size() > 1) {
            logger.debug("Checking the wait time in seconds to be used for the following commands : {}. If there are multiple commands sent at once," +
                    "then max wait time of those will be used", cmds);
        }

        for (final Command cmd : cmds) {
            if (cmd.getWait() > wait) {
                wait = cmd.getWait();
            }
        }
        return send(hostId, cmds, wait);
    }

    @Override
    public void reconnect(final long hostId) throws AgentUnavailableException {
        HostVO host = _hostDao.findById(hostId);
        if (host == null) {
            throw new CloudRuntimeException("Unable to find host: " + hostId);
        }

        if (host.getRemoved() != null) {
            throw new CloudRuntimeException(String.format(
                    "Host has already been removed: %s", host));
        }

        if (host.getStatus() == Status.Disconnected) {
            logger.debug("Host is already disconnected, no work to be done: {}", host);
            return;
        }

        if (host.getStatus() != Status.Up && host.getStatus() != Status.Alert && host.getStatus() != Status.Rebalancing) {
            throw new CloudRuntimeException(String.format(
                    "Unable to disconnect host because it is not in the correct state: host=%s; Status=%s",
                    host, host.getStatus()));
        }

        AgentAttache attache = findAttache(hostId);
        if (attache == null) {
            throw new CloudRuntimeException(String.format(
                    "Unable to disconnect host because it is not connected to this server: %s",
                    host));
        }
        disconnectWithoutInvestigation(attache, Event.ShutdownRequested);
    }

    @Override
    public void notifyMonitorsOfHostAboutToBeRemoved(long hostId) {
        for (final Pair<Integer, Listener> monitor : _hostMonitors) {
            logger.debug("Sending host about to be removed to listener: {}", monitor.second().getClass().getSimpleName());

            monitor.second().processHostAboutToBeRemoved(hostId);
        }
    }

    @Override
    public void notifyMonitorsOfRemovedHost(long hostId, long clusterId) {
        for (final Pair<Integer, Listener> monitor : _hostMonitors) {
            logger.debug("Sending host removed to listener: {}", monitor.second().getClass().getSimpleName());

            monitor.second().processHostRemoved(hostId, clusterId);
        }
    }

    public boolean executeUserRequest(final long hostId, final Event event) throws AgentUnavailableException {
        if (event == Event.AgentDisconnected) {
            AgentAttache attache;
            attache = findAttache(hostId);
            logger.debug("Received agent disconnect event for host {} ({})", hostId, attache);
            if (attache != null) {
                handleDisconnectWithoutInvestigation(attache, Event.AgentDisconnected, true, true);
            }
            return true;
        }
        if (event == Event.ShutdownRequested) {
            try {
                reconnect(hostId);
            } catch (CloudRuntimeException e) {
                logger.debug("Error on shutdown request for hostID: {} ({})", hostId, findAttache(hostId), e);
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isAgentAttached(final long hostId) {
        final AgentAttache agentAttache = findAttache(hostId);
        return agentAttache != null;
    }

    protected AgentAttache createAttacheForConnect(final HostVO host, final Link link) {
        logger.debug("create ConnectedAgentAttache for {}", host);
        final AgentAttache attache = new ConnectedAgentAttache(this, host.getId(), host.getUuid(), host.getName(), host.getHypervisorType(), link, host.isInMaintenanceStates());
        link.attach(attache);

        AgentAttache old;
        synchronized (_agents) {
            old = _agents.put(host.getId(), attache);
        }
        if (old != null) {
            old.disconnect(Status.Removed);
        }

        return attache;
    }

    private AgentAttache sendReadyAndGetAttache(HostVO host, ReadyCommand ready, Link link, StartupCommand[] startupCmds) throws ConnectionException {
        AgentAttache attache;
        GlobalLock joinLock = getHostJoinLock(host.getId());
        try {
            if (!joinLock.lock(60)) {
                throw new ConnectionException(true, String.format("Unable to acquire lock on host %s, to process agent connection", host));
            }

            logger.debug("Acquired lock on host {}, to process agent connection", host);
            attache = connectHostAgent(host, ready, link, startupCmds, joinLock);
        } finally {
            joinLock.releaseRef();
        }

        return attache;
    }

    private AgentAttache connectHostAgent(HostVO host, ReadyCommand ready, Link link, StartupCommand[] startupCmds, GlobalLock joinLock) throws ConnectionException {
        AgentAttache attache;
        try {
            final List<String> agentMSHostList = new ArrayList<>();
            String lbAlgorithm = null;
            if (startupCmds != null && startupCmds.length > 0) {
                final String agentMSHosts = startupCmds[0].getMsHostList();
                if (StringUtils.isNotEmpty(agentMSHosts)) {
                    String[] msHosts = agentMSHosts.split("@");
                    if (msHosts.length > 1) {
                        lbAlgorithm = msHosts[1];
                    }
                    agentMSHostList.addAll(Arrays.asList(msHosts[0].split(",")));
                }
            }

            if (!indirectAgentLB.compareManagementServerListAndLBAlgorithm(host.getId(), host.getDataCenterId(), agentMSHostList, lbAlgorithm)) {
                final List<String> newMSList = indirectAgentLB.getManagementServerList(host.getId(), host.getDataCenterId(), null);
                ready.setMsHostList(newMSList);
                String newLBAlgorithm = indirectAgentLB.getLBAlgorithmName();
                ready.setLbAlgorithm(newLBAlgorithm);
                logger.debug("Agent's management server host list or lb algorithm is not up to date, sending list and algorithm update: {}, {}", newMSList, newLBAlgorithm);
            }

            final List<String> avoidMsList = _mshostDao.listNonUpStateMsIPs();
            ready.setAvoidMsHostList(avoidMsList);
            ready.setLbCheckInterval(indirectAgentLB.getLBPreferredHostCheckInterval(host.getClusterId()));
            ready.setArch(host.getArch().getType());

            attache = createAttacheForConnect(host, link);
            attache = notifyMonitorsOfConnection(attache, startupCmds, false);
        } finally {
            joinLock.unlock();
        }

        return attache;
    }

    private AgentAttache handleConnectedAgent(final Link link, final StartupCommand[] startup) {
        AgentAttache attache = null;
        ReadyCommand ready = null;
        try {
            final HostVO host = _resourceMgr.createHostVOForConnectedAgent(startup);
            if (host != null) {
                checkHostArchOnCluster(host);
                ready = new ReadyCommand(host, NumbersUtil.enableHumanReadableSizes);
                attache = sendReadyAndGetAttache(host, ready, link, startup);
            }
        } catch (final Exception e) {
            logger.debug("Failed to handle host connection: ", e);
            ready = new ReadyCommand(null);
            ready.setDetails(e.toString());
        } finally {
            if (ready == null) {
                ready = new ReadyCommand(null);
            }
        }

        try {
            if (attache == null) {
                final Request readyRequest = new Request(-1, -1, ready, false);
                link.send(readyRequest.getBytes());
            } else {
                easySend(attache.getId(), ready);
            }
        } catch (final Exception e) {
            logger.debug("Failed to send ready command:", e);
        }
        return attache;
    }

    private void checkHostArchOnCluster(HostVO host) {
        Cluster cluster = _resourceMgr.getCluster(host.getClusterId());
        if (cluster != null && !cluster.getArch().equals(host.getArch())) {
            String msg = String.format("The host %s has arch %s and cannot be added to the %s cluster %s",
                    host.getName(), host.getArch().getType(), cluster.getArch().getType(), cluster.getName());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    protected class SimulateStartTask extends ManagedContextRunnable {
        ServerResource resource;
        Map<String, String> details;
        long id;
        String uuid;
        String name;

        public SimulateStartTask(final long id, String uuid, String name, final ServerResource resource, final Map<String, String> details) {
            this.id = id;
            this.resource = resource;
            this.details = details;
            this.uuid = uuid;
            this.name = name;
        }

        @Override
        protected void runInContext() {
            try {
                logger.debug("Simulating start for resource {} (id: {}, uuid: {}, name {})", resource.getName(), id, uuid, name);

                if (tapLoadingAgents(id, TapAgentsAction.Add)) {
                    try {
                        final AgentAttache agentattache = findAttache(id);
                        if (agentattache == null) {
                            logger.debug("Creating agent for host [id: {}, uuid: {}, name: {}]", id, uuid, name);
                            _resourceMgr.createHostAndAgent(id, resource, details, false, null, false);
                            logger.debug("Completed creating agent for host [id: {}, uuid: {}, name: {}", id, uuid, name);
                        } else {
                            logger.debug("Agent already created in another thread for host [id: {}, uuid: {}, name: {}], ignore this", id, uuid, name);
                        }
                    } finally {
                        tapLoadingAgents(id, TapAgentsAction.Del);
                    }
                } else {
                    logger.debug("Agent creation already getting processed in another thread for host [id: {}, uuid: {}, name: {}], ignore this", id, uuid, name);
                }
            } catch (final Exception e) {
                logger.warn("Unable to simulate start on resource [id: {}, uuid: {}, name: {}] name {}", id, uuid, name, resource.getName(), e);
            }
        }
    }

    protected class HandleAgentConnectTask extends ManagedContextRunnable {
        Link _link;
        Command[] _cmds;
        Request _request;

        HandleAgentConnectTask(final Link link, final Command[] cmds, final Request request) {
            _link = link;
            _cmds = cmds;
            _request = request;
        }

        @Override
        protected void runInContext() {
            _request.logD("Processing the first command ");
            final StartupCommand[] startups = new StartupCommand[_cmds.length];
            for (int i = 0; i < _cmds.length; i++) {
                startups[i] = (StartupCommand)_cmds[i];
            }

            final AgentAttache attache = handleConnectedAgent(_link, startups);
            if (attache == null) {
                logger.warn("Unable to create attache for agent: {}", _request);
            }
            unregisterNewConnection(_link.getSocketAddress());
        }
    }

    protected void connectAgent(final Link link, final Command[] cmds, final Request request) {
        // send startupanswer to agent in the very beginning, so agent can move on without waiting for the answer for an undetermined time, if we put this logic into another
        // thread pool.
        final StartupAnswer[] answers = new StartupAnswer[cmds.length];
        Command cmd;
        for (int i = 0; i < cmds.length; i++) {
            cmd = cmds[i];
            if (cmd instanceof StartupRoutingCommand || cmd instanceof StartupProxyCommand || cmd instanceof StartupSecondaryStorageCommand ||
                    cmd instanceof StartupStorageCommand) {
                answers[i] = new StartupAnswer((StartupCommand) cmds[i], 0, "", "", mgmtServiceConf.getPingInterval());
                break;
            }
        }
        Response response;
        response = new Response(request, answers[0], _nodeId, -1);
        try {
            link.send(response.toBytes());
        } catch (final ClosedChannelException e) {
            logger.debug("Failed to send startupanswer: {}", e.toString());
        }
        _connectExecutor.execute(new HandleAgentConnectTask(link, cmds, request));
    }

    public class AgentHandler extends Task {
        public AgentHandler(final Task.Type type, final Link link, final byte[] data) {
            super(type, link, data);
        }

        private void processHostHealthCheckResult(Boolean hostHealthCheckResult, long hostId) {
            if (hostHealthCheckResult == null) {
                return;
            }
            HostVO host = _hostDao.findById(hostId);
            if (host == null) {
                logger.error("Unable to find host with ID: {}", hostId);
                return;
            }
            if (!BooleanUtils.toBoolean(EnableKVMAutoEnableDisable.valueIn(host.getClusterId()))) {
                logger.debug("{} is disabled for the cluster {}, cannot process the health check result " +
                        "received for {}", EnableKVMAutoEnableDisable.key(), host.getClusterId(), host);
                return;
            }

            ResourceState.Event resourceEvent = hostHealthCheckResult ? ResourceState.Event.Enable : ResourceState.Event.Disable;

            try {
                logger.info("Host health check {}, auto {} KVM host: {}",
                        hostHealthCheckResult ? "succeeds" : "fails",
                        hostHealthCheckResult ? "enabling" : "disabling",
                        host);
                _resourceMgr.autoUpdateHostAllocationState(hostId, resourceEvent);
            } catch (NoTransitionException e) {
                logger.error("Cannot Auto {} host: {}", resourceEvent, host, e);
            }
        }

        private void processStartupRoutingCommand(StartupRoutingCommand startup, long hostId) {
            if (startup == null) {
                logger.error("Empty StartupRoutingCommand received");
                return;
            }
            Boolean hostHealthCheckResult = startup.getHostHealthCheckResult();
            processHostHealthCheckResult(hostHealthCheckResult, hostId);
        }

        private void processPingRoutingCommand(PingRoutingCommand pingRoutingCommand, long hostId) {
            if (pingRoutingCommand == null) {
                logger.error("Empty PingRoutingCommand received");
                return;
            }
            Boolean hostHealthCheckResult = pingRoutingCommand.getHostHealthCheckResult();
            processHostHealthCheckResult(hostHealthCheckResult, hostId);
        }

        protected void processRequest(final Link link, final Request request) {
            final AgentAttache attache = (AgentAttache)link.attachment();
            final Command[] cmds = request.getCommands();
            Command cmd = cmds[0];
            boolean logD = true;

            if (attache == null) {
                if (!(cmd instanceof StartupCommand)) {
                    logger.warn("Throwing away a request because it came through as the first command on a connect: {}", request);
                } else {
                    // submit the task for execution
                    request.logD("Scheduling the first command ");
                    connectAgent(link, cmds, request);
                }
                return;
            } else if (cmd instanceof StartupCommand) {
                connectAgent(link, cmds, request);
            }

            final long hostId = attache.getId();

            if (logger.isDebugEnabled()) {
                if (cmd instanceof PingRoutingCommand) {
                    logD = false;
                    logger.debug("Ping from Routing host {}", attache);
                    logger.trace("SeqA {}-{}: Processing {}", hostId, request.getSequence(), request);
                } else if (cmd instanceof PingCommand) {
                    logD = false;
                    logger.debug("Ping from {}", attache);
                    logger.trace("SeqA {}-{}: Processing {}", hostId, request.getSequence(), request);
                } else {
                    logger.debug("SeqA {}-{}: {}", hostId, request.getSequence(), request);
                }
            }

            final Answer[] answers = new Answer[cmds.length];
            for (int i = 0; i < cmds.length; i++) {
                cmd = cmds[i];
                Answer answer;
                try {
                    if (cmd instanceof StartupRoutingCommand) {
                        final StartupRoutingCommand startup = (StartupRoutingCommand) cmd;
                        processStartupRoutingCommand(startup, hostId);
                        answer = new StartupAnswer(startup, attache.getId(), attache.getUuid(), attache.getName(), mgmtServiceConf.getPingInterval());
                    } else if (cmd instanceof StartupProxyCommand) {
                        final StartupProxyCommand startup = (StartupProxyCommand) cmd;
                        answer = new StartupAnswer(startup, attache.getId(), attache.getUuid(), attache.getName(), mgmtServiceConf.getPingInterval());
                    } else if (cmd instanceof StartupSecondaryStorageCommand) {
                        final StartupSecondaryStorageCommand startup = (StartupSecondaryStorageCommand) cmd;
                        answer = new StartupAnswer(startup, attache.getId(), attache.getUuid(), attache.getName(), mgmtServiceConf.getPingInterval());
                    } else if (cmd instanceof StartupStorageCommand) {
                        final StartupStorageCommand startup = (StartupStorageCommand) cmd;
                        answer = new StartupAnswer(startup, attache.getId(), attache.getUuid(), attache.getName(), mgmtServiceConf.getPingInterval());
                    } else if (cmd instanceof ShutdownCommand) {
                        final ShutdownCommand shutdown = (ShutdownCommand)cmd;
                        final String reason = shutdown.getReason();
                        logger.info("Host {} has informed us that it is shutting down with reason {} and detail {}", attache, reason, shutdown.getDetail());
                        if (reason.equals(ShutdownCommand.Update)) {
                            // disconnectWithoutInvestigation(attache, Event.UpdateNeeded);
                            throw new CloudRuntimeException("Agent update not implemented");
                        } else if (reason.equals(ShutdownCommand.Requested)) {
                            disconnectWithoutInvestigation(attache, Event.ShutdownRequested);
                        }
                        return;
                    } else if (cmd instanceof AgentControlCommand) {
                        answer = handleControlCommand(attache, (AgentControlCommand)cmd);
                    } else {
                        handleCommands(attache, request.getSequence(), new Command[] {cmd});
                        if (cmd instanceof PingCommand) {
                            final long cmdHostId = ((PingCommand)cmd).getHostId();
                            boolean requestStartupCommand = false;

                            final HostVO host = _hostDao.findById(cmdHostId);
                            boolean gatewayAccessible = true;
                            // if the router is sending a ping, verify the
                            // gateway was pingable
                            if (cmd instanceof PingRoutingCommand) {
                                processPingRoutingCommand((PingRoutingCommand) cmd, hostId);
                                gatewayAccessible = ((PingRoutingCommand)cmd).isGatewayAccessible();

                                if (host != null) {
                                    if (!gatewayAccessible) {
                                        // alert that host lost connection to
                                        // gateway (cannot ping the default route)
                                        final DataCenterVO dcVO = _dcDao.findById(host.getDataCenterId());
                                        final HostPodVO podVO = _podDao.findById(host.getPodId());
                                        final String hostDesc = String.format("%s, availability zone: %s, pod: %s", host, dcVO, podVO);

                                        _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_ROUTING, host.getDataCenterId(), host.getPodId(), "Host lost connection to gateway, " + hostDesc,
                                                "Host [" + hostDesc + "] lost connection to gateway (default route) and is possibly having network connection issues.");
                                    } else {
                                        _alertMgr.clearAlert(AlertManager.AlertType.ALERT_TYPE_ROUTING, host.getDataCenterId(), host.getPodId());
                                    }
                                } else {
                                    logger.debug("Not processing {} for agent id={}; can't find the host in the DB", PingRoutingCommand.class.getSimpleName(), cmdHostId);
                                }
                            }
                            if (host != null && host.getStatus() != Status.Up && gatewayAccessible) {
                                requestStartupCommand = true;
                            }
                            final List<String> avoidMsList = _mshostDao.listNonUpStateMsIPs();
                            answer = new PingAnswer((PingCommand)cmd, avoidMsList, requestStartupCommand);

                            // Add or update reconcile tasks
                            reconcileCommandService.processCommand(cmd, answer);
                        } else if (cmd instanceof ReadyAnswer) {
                            final HostVO host = _hostDao.findById(attache.getId());
                            if (host == null) {
                                logger.debug("Cant not find host with id: {} ({})", attache.getId(), attache);
                            }
                            answer = new Answer(cmd);
                        } else {
                            answer = new Answer(cmd);
                        }
                    }
                } catch (final Throwable th) {
                    logger.error("Caught: ", th);
                    answer = new Answer(cmd, false, th.getMessage());
                }
                answers[i] = answer;
            }

            final Response response = new Response(request, answers, _nodeId, attache.getId());
            if (logD) {
                logger.debug("SeqA {}-: Sending {}", attache.getId(), response.getSequence(), response);
            } else {
                logger.trace("SeqA {}-: Sending {} {}", response.getSequence(), response, attache.getId());
            }
            try {
                link.send(response.toBytes());
            } catch (final ClosedChannelException e) {
                logger.error("Unable to send response because connection is closed: {}", response);
            }
        }

        protected void processResponse(final Link link, final Response response) {
            final AgentAttache attache = (AgentAttache)link.attachment();
            if (attache == null) {
                logger.warn("Unable to process: {}", response);
            } else if (!attache.processAnswers(response.getSequence(), response)) {
                logger.info("Host {} - Seq {}: Response is not processed: {}", attache, response.getSequence(), response);
            }
        }

        @Override
        protected void doTask(final Task task) throws TaskExecutionException {
            try (TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB)) {
                final Type type = task.getType();
                if (type == Type.DATA) {
                    final byte[] data = task.getData();
                    try {
                        final Request event = Request.parse(data);
                        if (event instanceof Response) {
                            processResponse(task.getLink(), (Response) event);
                        } else {
                            processRequest(task.getLink(), event);
                        }
                    } catch (final UnsupportedVersionException e) {
                        logger.warn(e.getMessage());
                        // upgradeAgent(task.getLink(), data, e.getReason());
                    } catch (final ClassNotFoundException e) {
                        final String message = String.format("Exception occurred when executing tasks! Error '%s'", e.getMessage());
                        logger.error(message);
                        throw new TaskExecutionException(message, e);
                    }
                } else if (type == Type.CONNECT) {
                } else if (type == Type.DISCONNECT) {
                    final Link link = task.getLink();
                    final AgentAttache attache = (AgentAttache) link.attachment();
                    if (attache != null) {
                        disconnectWithInvestigation(attache, Event.AgentDisconnected);
                    } else {
                        logger.info("Connection from {} closed but no cleanup was done.", link.getIpAddress());
                        link.close();
                        link.terminated();
                    }
                }
            }
        }
    }

    protected AgentManagerImpl() {
    }

    public boolean tapLoadingAgents(final Long hostId, final TapAgentsAction action) {
        synchronized (_loadingAgents) {
            if (action == TapAgentsAction.Add) {
                if (_loadingAgents.contains(hostId)) {
                    return false;
                } else {
                    _loadingAgents.add(hostId);
                }
            } else if (action == TapAgentsAction.Del) {
                _loadingAgents.remove(hostId);
            } else if (action == TapAgentsAction.Contains) {
                return _loadingAgents.contains(hostId);
            } else {
                throw new CloudRuntimeException("Unknown TapAgentsAction " + action);
            }
        }
        return true;
    }

    @Override
    public boolean agentStatusTransitTo(final HostVO host, final Status.Event e, final long msId) {
        logger.debug("[Resource state = {}, Agent event = , Host = {}]",
                host.getResourceState(), e.toString(), host);

        host.setManagementServerId(msId);
        try {
            return _statusStateMachine.transitTo(host, e, host.getId(), _hostDao);
        } catch (final NoTransitionException e1) {
            logger.debug("Cannot transit agent status with event {} for host {}, management server id is {}", e, host, msId);
            throw new CloudRuntimeException(String.format(
                    "Cannot transit agent status with event %s for host %s, management server id is %d, %s", e, host, msId, e1.getMessage()));
        }
    }

    public boolean disconnectAgent(final HostVO host, final Status.Event e, final long msId) {
        host.setDisconnectedOn(new Date());
        if (e.equals(Status.Event.Remove)) {
            host.setGuid(null);
            host.setClusterId(null);
        }

        return agentStatusTransitTo(host, e, msId);
    }

    protected void disconnectWithoutInvestigation(final AgentAttache attache, final Status.Event event) {
        _executor.submit(new DisconnectTask(attache, event, false));
    }

    public void disconnectWithInvestigation(final AgentAttache attache, final Status.Event event) {
        _executor.submit(new DisconnectTask(attache, event, true));
    }

    protected boolean isHostOwnerSwitched(final long hostId) {
        final HostVO host = _hostDao.findById(hostId);
        if (host == null) {
            logger.warn("Can't find the host {}", hostId);
            return false;
        }
        return isHostOwnerSwitched(host);
    }

    protected boolean isHostOwnerSwitched(final HostVO host) {
        if (host.getStatus() == Status.Up && host.getManagementServerId() != null && host.getManagementServerId() != _nodeId) {
            return true;
        }
        return false;
    }

    private void disconnectInternal(final long hostId, final Status.Event event, final boolean investigate) {
        final AgentAttache attache = findAttache(hostId);

        if (attache != null) {
            if (!investigate) {
                disconnectWithoutInvestigation(attache, event);
            } else {
                disconnectWithInvestigation(attache, event);
            }
        } else {
            /* Agent is still in connecting process, don't allow to disconnect right away */
            if (tapLoadingAgents(hostId, TapAgentsAction.Contains)) {
                logger.info("Host {} is being loaded no disconnects needed.", hostId);
                return;
            }

            final HostVO host = _hostDao.findById(hostId);
            if (host != null && host.getRemoved() == null) {
                disconnectAgent(host, event, _nodeId);
            }
        }
    }

    @Override
    public void disconnectWithInvestigation(final long hostId, final Status.Event event) {
        disconnectInternal(hostId, event, true);
    }

    @Override
    public void disconnectWithoutInvestigation(final long hostId, final Status.Event event) {
        disconnectInternal(hostId, event, false);
    }

    @Override
    public boolean handleDirectConnectAgent(final Host host, final StartupCommand[] cmds, final ServerResource resource, final boolean forRebalance, boolean newHost) throws ConnectionException {
        AgentAttache attache;

        attache = createAttacheForDirectConnect(host, resource);
        final StartupAnswer[] answers = new StartupAnswer[cmds.length];
        for (int i = 0; i < answers.length; i++) {
            answers[i] = new StartupAnswer(cmds[i], attache.getId(), attache.getUuid(), attache.getName(), mgmtServiceConf.getPingInterval());
        }
        attache.process(answers);

        if (newHost) {
            notifyMonitorsOfNewlyAddedHost(host.getId());
        }

        attache = notifyMonitorsOfConnection(attache, cmds, forRebalance);

        return attache != null;
    }

    @Override
    public void pullAgentToMaintenance(final long hostId) {
        final AgentAttache attache = findAttache(hostId);
        if (attache != null) {
            attache.setMaintenanceMode(true);
            // Now cancel all of the commands except for the active one.
            attache.cancelAllCommands(Status.Disconnected, false);
        }
    }

    @Override
    public void pullAgentOutMaintenance(final long hostId) {
        final AgentAttache attache = findAttache(hostId);
        if (attache != null) {
            attache.setMaintenanceMode(false);
        }
    }

    public ScheduledExecutorService getDirectAgentPool() {
        return _directAgentExecutor;
    }

    public ScheduledExecutorService getCronJobPool() {
        return _cronJobExecutor;
    }

    public int getDirectAgentThreadCap() {
        return _directAgentThreadCap;
    }

    public Long getAgentPingTime(final long agentId) {
        return _pingMap.get(agentId);
    }

    public void pingBy(final long agentId) {
        // Update PingMap with the latest time if agent entry exists in the PingMap
        if (_pingMap.replace(agentId, InaccurateClock.getTimeInSeconds()) == null) {
            logger.info("PingMap for agent: {} ({}) will not be updated because agent is no longer in the PingMap", agentId, findAttache(agentId));
        }
    }

    protected class MonitorTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            logger.trace("Agent Monitor is started.");

            try {
                final List<Long> behindAgents = findAgentsBehindOnPing();
                for (final Long agentId : behindAgents) {
                    final QueryBuilder<HostVO> sc = QueryBuilder.create(HostVO.class);
                    sc.and(sc.entity().getId(), Op.EQ, agentId);
                    final HostVO h = sc.find();
                    if (h != null) {
                        final ResourceState resourceState = h.getResourceState();
                        if (resourceState == ResourceState.Disabled || resourceState == ResourceState.Maintenance) {
                            /*
                             * Host is in non-operation state, so no investigation and direct put agent to Disconnected
                             */
                            logger.debug("Ping timeout but agent {} is in resource state of {}, so no investigation", h, resourceState);
                            disconnectWithoutInvestigation(agentId, Event.ShutdownRequested);
                        } else {
                            final HostVO host = _hostDao.findById(agentId);
                            if (host != null
                                    && (host.getType() == Host.Type.ConsoleProxy || host.getType() == Host.Type.SecondaryStorageVM || host.getType() == Host.Type.SecondaryStorageCmdExecutor)) {

                                logger.warn("Disconnect agent for CPVM/SSVM due to physical connection close. host: {}", host);
                                disconnectWithoutInvestigation(agentId, Event.ShutdownRequested);
                            } else {
                                logger.debug("Ping timeout for agent {}, do investigation", h);
                                disconnectWithInvestigation(agentId, Event.PingTimeout);
                            }
                        }
                    }
                }

                final QueryBuilder<HostVO> sc = QueryBuilder.create(HostVO.class);
                sc.and(sc.entity().getResourceState(), Op.IN,
                        ResourceState.PrepareForMaintenance,
                        ResourceState.ErrorInPrepareForMaintenance);
                final List<HostVO> hosts = sc.list();

                for (final HostVO host : hosts) {
                    if (_resourceMgr.checkAndMaintain(host.getId())) {
                        final DataCenterVO dcVO = _dcDao.findById(host.getDataCenterId());
                        final HostPodVO podVO = _podDao.findById(host.getPodId());
                        final String hostDesc = "name: " + host.getName() + " (id:" + host.getId() + "), availability zone: " + dcVO.getName() + ", pod: " + podVO.getName();
                        _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, host.getDataCenterId(), host.getPodId(), "Migration Complete for host " + hostDesc,
                                "Host [" + hostDesc + "] is ready for maintenance");
                    }
                }
            } catch (final Throwable th) {
                logger.error("Caught the following exception: ", th);
            }

            logger.trace("Agent Monitor is leaving the building!");
        }

        protected List<Long> findAgentsBehindOnPing() {
            final List<Long> agentsBehind = new ArrayList<>();
            final long cutoffTime = InaccurateClock.getTimeInSeconds() - mgmtServiceConf.getTimeout();
            for (final Map.Entry<Long, Long> entry : _pingMap.entrySet()) {
                if (entry.getValue() < cutoffTime) {
                    agentsBehind.add(entry.getKey());
                }
            }

            if (!agentsBehind.isEmpty()) {
                logger.info("Found the following agents behind on ping: {}", agentsBehind);
            }

            return agentsBehind;
        }
    }

    protected class AgentNewConnectionsMonitorTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            logger.trace("Agent New Connections Monitor is started.");
            final int cleanupTime = RemoteAgentNewConnectionsMonitorInterval.value();
            Set<Map.Entry<String, Long>> entrySet = newAgentConnections.entrySet();
            long cutOff = System.currentTimeMillis() - (cleanupTime * 1000L);
            List<String> expiredConnections = newAgentConnections.entrySet()
                    .stream()
                    .filter(e -> e.getValue() <= cutOff)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            logger.debug("Currently {} active new connections, of which {} have expired - {}",
                    entrySet.size(),
                    expiredConnections.size(),
                    StringUtils.join(expiredConnections));
            for (String connection : expiredConnections) {
                logger.trace("Cleaning up new agent connection for {}", connection);
                newAgentConnections.remove(connection);
            }
        }
    }

    protected class BehindOnPingListener implements Listener {
        @Override
        public boolean isRecurring() {
            return true;
        }

        @Override
        public boolean processAnswers(final long agentId, final long seq, final Answer[] answers) {
            return false;
        }

        @Override
        public boolean processCommands(final long agentId, final long seq, final Command[] commands) {
            final boolean processed = false;
            for (final Command cmd : commands) {
                if (cmd instanceof PingCommand) {
                    pingBy(agentId);
                }
            }
            return processed;
        }

        @Override
        public AgentControlAnswer processControlCommand(final long agentId, final AgentControlCommand cmd) {
            return null;
        }

        @Override
        public void processHostAdded(long hostId) {
        }

        @Override
        public void processConnect(final Host host, final StartupCommand cmd, final boolean forRebalance) {
            if (host.getType().equals(Host.Type.TrafficMonitor) || host.getType().equals(Host.Type.SecondaryStorage)) {
                return;
            }

            // NOTE: We don't use pingBy here because we're initiating.
            _pingMap.put(host.getId(), InaccurateClock.getTimeInSeconds());
        }

        @Override
        public boolean processDisconnect(final long agentId, final Status state) {
            _pingMap.remove(agentId);
            return true;
        }

        @Override
        public void processHostAboutToBeRemoved(long hostId) {
        }

        @Override
        public void processHostRemoved(long hostId, long clusterId) {
        }

        @Override
        public boolean processTimeout(final long agentId, final long seq) {
            return true;
        }

        @Override
        public int getTimeout() {
            return -1;
        }

    }

    @Override
    public String getConfigComponentName() {
        return AgentManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] { CheckTxnBeforeSending, Workers, Port, Wait, AlertWait, DirectAgentLoadSize,
                DirectAgentPoolSize, DirectAgentThreadCap, EnableKVMAutoEnableDisable, ReadyCommandWait,
                GranularWaitTimeForCommands, RemoteAgentSslHandshakeTimeout, RemoteAgentMaxConcurrentNewConnections,
                RemoteAgentNewConnectionsMonitorInterval };
    }

    protected class SetHostParamsListener implements Listener {
        @Override
        public boolean isRecurring() {
            return false;
        }

        @Override
        public boolean processAnswers(final long agentId, final long seq, final Answer[] answers) {
            return false;
        }

        @Override
        public boolean processCommands(final long agentId, final long seq, final Command[] commands) {
            return false;
        }

        @Override
        public AgentControlAnswer processControlCommand(final long agentId, final AgentControlCommand cmd) {
            return null;
        }

        @Override
        public void processHostAdded(long hostId) {
        }

        @Override
        public void processConnect(final Host host, final StartupCommand cmd, final boolean forRebalance) {
            if (!(cmd instanceof StartupRoutingCommand) || cmd.isConnectionTransferred()) {
                return;
            }

            if (((StartupRoutingCommand)cmd).getHypervisorType() == HypervisorType.KVM || ((StartupRoutingCommand)cmd).getHypervisorType() == HypervisorType.LXC) {
                Map<String, String> params = new HashMap<>();
                params.put(Config.RouterAggregationCommandEachTimeout.toString(), _configDao.getValue(Config.RouterAggregationCommandEachTimeout.toString()));
                params.put(Config.MigrateWait.toString(), _configDao.getValue(Config.MigrateWait.toString()));
                params.put(NetworkOrchestrationService.TUNGSTEN_ENABLED.key(), String.valueOf(NetworkOrchestrationService.TUNGSTEN_ENABLED.valueIn(host.getDataCenterId())));
                params.put(ReconcileCommandService.ReconcileCommandsEnabled.key(), String.valueOf(_reconcileCommandsEnabled));

                    try {
                        SetHostParamsCommand cmds = new SetHostParamsCommand(params);
                        Commands c = new Commands(cmds);
                        send(host.getId(), c, this);
                    } catch (AgentUnavailableException e) {
                        logger.debug("Failed to send host params on host: {}", host);
                    }
                }
            }

        @Override
        public boolean processDisconnect(final long agentId, final Status state) {
            return true;
        }

        @Override
        public void processHostAboutToBeRemoved(long hostId) {
        }

        @Override
        public void processHostRemoved(long hostId, long clusterId) {
        }

        @Override
        public boolean processTimeout(final long agentId, final long seq) {
            return false;
        }

        @Override
        public int getTimeout() {
            return -1;
        }

    }

    protected Map<Long, List<Long>> getHostsPerZone() {
        List<HostVO> allHosts = _resourceMgr.listAllHostsInAllZonesByType(Host.Type.Routing);
        if (allHosts == null) {
            return null;
        }
        Map<Long, List<Long>> hostsByZone = new HashMap<>();
        for (HostVO host : allHosts) {
            if (host.getHypervisorType() == HypervisorType.KVM || host.getHypervisorType() == HypervisorType.LXC) {
                Long zoneId = host.getDataCenterId();
                List<Long> hostIds = hostsByZone.get(zoneId);
                if (hostIds == null) {
                    hostIds = new ArrayList<>();
                }
                hostIds.add(host.getId());
                hostsByZone.put(zoneId, hostIds);
            }
        }
        return hostsByZone;
    }

    private void sendCommandToAgents(Map<Long, List<Long>> hostsPerZone, Map<String, String> params) {
        SetHostParamsCommand cmds = new SetHostParamsCommand(params);
        for (Long zoneId : hostsPerZone.keySet()) {
            List<Long> hostIds = hostsPerZone.get(zoneId);
            for (Long hostId : hostIds) {
                Answer answer = easySend(hostId, cmds);
                if (answer == null || !answer.getResult()) {
                    logger.error("Error sending parameters to agent {} ({})", hostId, findAttache(hostId));
                }
            }
        }
    }

    @Override
    public void propagateChangeToAgents(Map<String, String> params) {
        if (params != null && ! params.isEmpty()) {
            logger.debug("Propagating changes on host parameters to the agents");
            Map<Long, List<Long>> hostsPerZone = getHostsPerZone();
            sendCommandToAgents(hostsPerZone, params);
        }
    }

    @Override
    public boolean transferDirectAgentsFromMS(String fromMsUuid, long fromMsId, long timeoutDurationInMs, boolean excludeHostsInMaintenance) {
        return true;
    }

    private GlobalLock getHostJoinLock(Long hostId) {
        return GlobalLock.getInternLock(String.format("%s-%s", "Host-Join", hostId));
    }

    public boolean isReconcileCommandsEnabled(HypervisorType hypervisorType) {
        return _reconcileCommandsEnabled && ReconcileCommandService.SupportedHypervisorTypes.contains(hypervisorType);
    }

    public void updateReconcileCommandsIfNeeded(long requestSeq, Command[] commands, Command.State state) {
        if (!_reconcileCommandsEnabled) {
            return;
        }
        for (Command command: commands) {
            if (command.isReconcile()) {
                reconcileCommandService.updateReconcileCommand(requestSeq, command, null, state, null);
            }
        }
    }

    public Pair<Command.State, Answer> getStateAndAnswerOfReconcileCommand(long requestSeq, Command command) {
        ReconcileCommandVO reconcileCommandVO = reconcileCommandDao.findCommand(requestSeq, command.toString());
        if (reconcileCommandVO == null) {
            return null;
        }
        Command.State state = reconcileCommandVO.getStateByAgent();
        if (reconcileCommandVO.getAnswerName() == null || reconcileCommandVO.getAnswerInfo() == null) {
            return new Pair<>(state, null);
        }
        Answer answer = ReconcileCommandUtils.parseAnswerFromAnswerInfo(reconcileCommandVO.getAnswerName(), reconcileCommandVO.getAnswerInfo());
        return new Pair<>(state, answer);
    }

    public Integer getReconcileInterval() {
        return _reconcileCommandInterval;
    }
}
