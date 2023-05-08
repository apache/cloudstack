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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.configuration.Config;
import com.cloud.utils.NumbersUtil;
import org.apache.cloudstack.agent.lb.IndirectAgentLB;
import org.apache.cloudstack.ca.CAManager;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobExecutionContext;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.outofbandmanagement.dao.OutOfBandManagementDao;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

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
import com.cloud.resource.Discoverer;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.resource.ServerResource;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
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
import org.apache.commons.lang3.StringUtils;

/**
 * Implementation of the Agent Manager. This class controls the connection to the agents.
 **/
public class AgentManagerImpl extends ManagerBase implements AgentManager, HandlerFactory, Configurable {
    protected static final Logger s_logger = Logger.getLogger(AgentManagerImpl.class);

    /**
     * _agents is a ConcurrentHashMap, but it is used from within a synchronized block. This will be reported by findbugs as JLM_JSR166_UTILCONCURRENT_MONITORENTER. Maybe a
     * ConcurrentHashMap is not the right thing to use here, but i'm not sure so i leave it alone.
     */
    protected ConcurrentHashMap<Long, AgentAttache> _agents = new ConcurrentHashMap<Long, AgentAttache>(10007);
    protected List<Pair<Integer, Listener>> _hostMonitors = new ArrayList<Pair<Integer, Listener>>(17);
    protected List<Pair<Integer, Listener>> _cmdMonitors = new ArrayList<Pair<Integer, Listener>>(17);
    protected List<Pair<Integer, StartupCommandProcessor>> _creationMonitors = new ArrayList<Pair<Integer, StartupCommandProcessor>>(17);
    protected List<Long> _loadingAgents = new ArrayList<Long>();
    private int _monitorId = 0;
    private final Lock _agentStatusLock = new ReentrantLock();

    @Inject
    protected CAManager caService;
    @Inject
    protected EntityManager _entityMgr;

    protected NioServer _connection;
    @Inject
    protected HostDao _hostDao = null;
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
    protected HypervisorGuruManager _hvGuruMgr;

    @Inject
    protected IndirectAgentLB indirectAgentLB;

    protected int _retry = 2;

    protected long _nodeId = -1;

    protected ExecutorService _executor;
    protected ThreadPoolExecutor _connectExecutor;
    protected ScheduledExecutorService _directAgentExecutor;
    protected ScheduledExecutorService _cronJobExecutor;
    protected ScheduledExecutorService _monitorExecutor;

    private int _directAgentThreadCap;

    protected StateMachine2<Status, Status.Event, Host> _statusStateMachine = Status.getStateMachine();
    private final ConcurrentHashMap<Long, Long> _pingMap = new ConcurrentHashMap<Long, Long>(10007);

    @Inject
    ResourceManager _resourceMgr;
    @Inject
    ManagementServiceConfiguration mgmtServiceConf;

    protected final ConfigKey<Integer> Workers = new ConfigKey<Integer>("Advanced", Integer.class, "workers", "5",
            "Number of worker threads handling remote agent connections.", false);
    protected final ConfigKey<Integer> Port = new ConfigKey<Integer>("Advanced", Integer.class, "port", "8250", "Port to listen on for remote agent connections.", false);
    protected final ConfigKey<Integer> AlertWait = new ConfigKey<Integer>("Advanced", Integer.class, "alert.wait", "1800",
            "Seconds to wait before alerting on a disconnected agent", true);
    protected final ConfigKey<Integer> DirectAgentLoadSize = new ConfigKey<Integer>("Advanced", Integer.class, "direct.agent.load.size", "16",
            "The number of direct agents to load each time", false);
    protected final ConfigKey<Integer> DirectAgentPoolSize = new ConfigKey<Integer>("Advanced", Integer.class, "direct.agent.pool.size", "500",
            "Default size for DirectAgentPool", false);
    protected final ConfigKey<Float> DirectAgentThreadCap = new ConfigKey<Float>("Advanced", Float.class, "direct.agent.thread.cap", "1",
            "Percentage (as a value between 0 and 1) of direct.agent.pool.size to be used as upper thread cap for a single direct agent to process requests", false);
    protected final ConfigKey<Boolean> CheckTxnBeforeSending = new ConfigKey<Boolean>("Developer", Boolean.class, "check.txn.before.sending.agent.commands", "false",
            "This parameter allows developers to enable a check to see if a transaction wraps commands that are sent to the resource.  This is not to be enabled on production systems.", true);

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {

        s_logger.info("Ping Timeout is " + mgmtServiceConf.getPingTimeout());

        final int threads = DirectAgentLoadSize.value();

        _nodeId = ManagementServerNode.getManagementServerId();
        s_logger.info("Configuring AgentManagerImpl. management server node id(msid): " + _nodeId);

        final long lastPing = (System.currentTimeMillis() >> 10) - mgmtServiceConf.getTimeout();
        _hostDao.markHostsAsDisconnected(_nodeId, lastPing);

        registerForHostEvents(new BehindOnPingListener(), true, true, false);

        registerForHostEvents(new SetHostParamsListener(), true, true, false);

        _executor = new ThreadPoolExecutor(threads, threads, 60l, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory("AgentTaskPool"));

        _connectExecutor = new ThreadPoolExecutor(100, 500, 60l, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory("AgentConnectTaskPool"));
        // allow core threads to time out even when there are no items in the queue
        _connectExecutor.allowCoreThreadTimeOut(true);

        _connection = new NioServer("AgentManager", Port.value(), Workers.value() + 10, this, caService);
        s_logger.info("Listening on " + Port.value() + " with " + Workers.value() + " workers");

        // executes all agent commands other than cron and ping
        _directAgentExecutor = new ScheduledThreadPoolExecutor(DirectAgentPoolSize.value(), new NamedThreadFactory("DirectAgent"));
        // executes cron and ping agent commands
        _cronJobExecutor = new ScheduledThreadPoolExecutor(DirectAgentPoolSize.value(), new NamedThreadFactory("DirectAgentCronJob"));
        s_logger.debug("Created DirectAgentAttache pool with size: " + DirectAgentPoolSize.value());
        _directAgentThreadCap = Math.round(DirectAgentPoolSize.value() * DirectAgentThreadCap.value()) + 1; // add 1 to always make the value > 0

        _monitorExecutor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("AgentMonitor"));

        return true;
    }

    @Override
    public Task create(final Task.Type type, final Link link, final byte[] data) {
        return new AgentHandler(type, link, data);
    }

    @Override
    public int registerForHostEvents(final Listener listener, final boolean connections, final boolean commands, final boolean priority) {
        synchronized (_hostMonitors) {
            _monitorId++;
            if (connections) {
                if (priority) {
                    _hostMonitors.add(0, new Pair<Integer, Listener>(_monitorId, listener));
                } else {
                    _hostMonitors.add(new Pair<Integer, Listener>(_monitorId, listener));
                }
            }
            if (commands) {
                if (priority) {
                    _cmdMonitors.add(0, new Pair<Integer, Listener>(_monitorId, listener));
                } else {
                    _cmdMonitors.add(new Pair<Integer, Listener>(_monitorId, listener));
                }
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Registering listener " + listener.getClass().getSimpleName() + " with id " + _monitorId);
            }
            return _monitorId;
        }
    }

    @Override
    public int registerForInitialConnects(final StartupCommandProcessor creator, final boolean priority) {
        synchronized (_hostMonitors) {
            _monitorId++;
            if (priority) {
                _creationMonitors.add(0, new Pair<Integer, StartupCommandProcessor>(_monitorId, creator));
            } else {
                _creationMonitors.add(new Pair<Integer, StartupCommandProcessor>(_monitorId, creator));
            }
            return _monitorId;
        }
    }

    @Override
    public void unregisterForHostEvents(final int id) {
        s_logger.debug("Deregistering " + id);
        _hostMonitors.remove(id);
    }

    private AgentControlAnswer handleControlCommand(final AgentAttache attache, final AgentControlCommand cmd) {
        AgentControlAnswer answer = null;

        for (final Pair<Integer, Listener> listener : _cmdMonitors) {
            answer = listener.second().processControlCommand(attache.getId(), cmd);

            if (answer != null) {
                return answer;
            }
        }

        s_logger.warn("No handling of agent control command: " + cmd + " sent from " + attache.getId());
        return new AgentControlAnswer(cmd);
    }

    public void handleCommands(final AgentAttache attache, final long sequence, final Command[] cmds) {
        for (final Pair<Integer, Listener> listener : _cmdMonitors) {
            final boolean processed = listener.second().processCommands(attache.getId(), sequence, cmds);
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("SeqA " + attache.getId() + "-" + sequence + ": " + (processed ? "processed" : "not processed") + " by " + listener.getClass());
            }
        }
    }

    public void notifyAnswersToMonitors(final long agentId, final long seq, final Answer[] answers) {
        for (final Pair<Integer, Listener> listener : _cmdMonitors) {
            listener.second().processAnswers(agentId, seq, answers);
        }
    }

    public AgentAttache findAttache(final long hostId) {
        AgentAttache attache = null;
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
                            host.getUuid(), e.getLocalizedMessage());
                    s_logger.error(errorMsg);
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug(errorMsg, e);
                    }
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
        if (answers != null && !(answers[0] instanceof UnsupportedAnswer)) {
            return answers[0];
        }

        if (answers != null && answers[0] instanceof UnsupportedAnswer) {
            s_logger.warn("Unsupported Command: " + answers[0].getDetails());
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
        String logcontextid = (String) MDC.get("logcontextid");
        if (StringUtils.isNotEmpty(logcontextid)) {
            cmd.setContextParam("logid", logcontextid);
        }
    }

    /**
     * @param commands
     * @return
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
     * @param commands
     * @param cmds
     */
    private void setEmptyAnswers(final Commands commands, final Command[] cmds) {
        if (cmds.length == 0) {
            commands.setAnswers(new Answer[0]);
        }
    }

    @Override
    public Answer[] send(final Long hostId, final Commands commands, int timeout) throws AgentUnavailableException, OperationTimedoutException {
        assert hostId != null : "Who's not checking the agent id before sending?  ... (finger wagging)";
        if (hostId == null) {
            throw new AgentUnavailableException(-1);
        }

        if (timeout <= 0) {
            timeout = Wait.value();
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
        final Answer[] answers = agent.send(req, timeout);
        notifyAnswersToMonitors(hostId, req.getSequence(), answers);
        commands.setAnswers(answers);
        return answers;
    }

    protected Status investigate(final AgentAttache agent) {
        final Long hostId = agent.getId();
        final HostVO host = _hostDao.findById(hostId);
        if (host != null && host.getType() != null && !host.getType().isVirtual()) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("checking if agent (" + hostId + ") is alive");
            }
            final Answer answer = easySend(hostId, new CheckHealthCommand());
            if (answer != null && answer.getResult()) {
                final Status status = Status.Up;
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("agent (" + hostId + ") responded to checkHeathCommand, reporting that agent is " + status);
                }
                return status;
            }
            return _haMgr.investigate(hostId);
        }
        return Status.Alert;
    }

    protected AgentAttache getAttache(final Long hostId) throws AgentUnavailableException {
        if (hostId == null) {
            return null;
        }
        final AgentAttache agent = findAttache(hostId);
        if (agent == null) {
            s_logger.debug("Unable to find agent for " + hostId);
            throw new AgentUnavailableException("Unable to find agent ", hostId);
        }

        return agent;
    }

    @Override
    public long send(final Long hostId, final Commands commands, final Listener listener) throws AgentUnavailableException {
        final AgentAttache agent = getAttache(hostId);
        if (agent.isClosed()) {
            throw new AgentUnavailableException("Agent " + agent.getId() + " is closed", agent.getId());
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
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Remove Agent : " + hostId);
        }
        AgentAttache removed = null;
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
            s_logger.debug("Agent for host " + hostId + " is created when it is being disconnected");
        }
        if (removed != null) {
            removed.disconnect(nextState);
        }

        for (final Pair<Integer, Listener> monitor : _hostMonitors) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Sending Disconnect to listener: " + monitor.second().getClass().getName());
            }
            monitor.second().processDisconnect(hostId, nextState);
        }
    }

    @Override
    public void notifyMonitorsOfNewlyAddedHost(long hostId) {
        for (final Pair<Integer, Listener> monitor : _hostMonitors) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Sending host added to listener: " + monitor.second().getClass().getSimpleName());
            }

            monitor.second().processHostAdded(hostId);
        }
    }

    protected AgentAttache notifyMonitorsOfConnection(final AgentAttache attache, final StartupCommand[] cmd, final boolean forRebalance) throws ConnectionException {
        final long hostId = attache.getId();
        final HostVO host = _hostDao.findById(hostId);
        for (final Pair<Integer, Listener> monitor : _hostMonitors) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Sending Connect to listener: " + monitor.second().getClass().getSimpleName());
            }
            for (int i = 0; i < cmd.length; i++) {
                try {
                    monitor.second().processConnect(host, cmd[i], forRebalance);
                } catch (final Exception e) {
                    if (e instanceof ConnectionException) {
                        final ConnectionException ce = (ConnectionException)e;
                        if (ce.isSetupError()) {
                            s_logger.warn("Monitor " + monitor.second().getClass().getSimpleName() + " says there is an error in the connect process for " + hostId + " due to " + e.getMessage());
                            handleDisconnectWithoutInvestigation(attache, Event.AgentDisconnected, true, true);
                            throw ce;
                        } else {
                            s_logger.info("Monitor " + monitor.second().getClass().getSimpleName() + " says not to continue the connect process for " + hostId + " due to " + e.getMessage());
                            handleDisconnectWithoutInvestigation(attache, Event.ShutdownRequested, true, true);
                            return attache;
                        }
                    } else if (e instanceof HypervisorVersionChangedException) {
                        handleDisconnectWithoutInvestigation(attache, Event.ShutdownRequested, true, true);
                        throw new CloudRuntimeException("Unable to connect " + attache.getId(), e);
                    } else {
                        s_logger.error("Monitor " + monitor.second().getClass().getSimpleName() + " says there is an error in the connect process for " + hostId + " due to " + e.getMessage(), e);
                        handleDisconnectWithoutInvestigation(attache, Event.AgentDisconnected, true, true);
                        throw new CloudRuntimeException("Unable to connect " + attache.getId(), e);
                    }
                }
            }
        }

        final Long dcId = host.getDataCenterId();
        final ReadyCommand ready = new ReadyCommand(dcId, host.getId(), NumbersUtil.enableHumanReadableSizes);
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
                s_logger.debug(String.format("Got HOST_UEFI_ENABLE [%s] for hostId [%s]:", uefiEnabled, host.getUuid()));
                if (uefiEnabled != null) {
                    _hostDao.loadDetails(host);
                    if (!uefiEnabled.equals(host.getDetails().get(Host.HOST_UEFI_ENABLE))) {
                        host.getDetails().put(Host.HOST_UEFI_ENABLE, uefiEnabled);
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
        startDirectlyConnectedHosts();

        if (_connection != null) {
            try {
                _connection.start();
            } catch (final NioConnectionException e) {
                s_logger.error("Error when connecting to the NioServer!", e);
            }
        }

        _monitorExecutor.scheduleWithFixedDelay(new MonitorTask(), mgmtServiceConf.getPingInterval(), mgmtServiceConf.getPingInterval(), TimeUnit.SECONDS);

        return true;
    }

    public void startDirectlyConnectedHosts() {
        final List<HostVO> hosts = _resourceMgr.findDirectlyConnectedHosts();
        for (final HostVO host : hosts) {
            loadDirectlyConnectedHost(host, false);
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
            s_logger.warn("Unable to find class " + host.getResource(), e);
        } catch (final InstantiationException e) {
            s_logger.warn("Unable to instantiate class " + host.getResource(), e);
        } catch (final IllegalAccessException e) {
            s_logger.warn("Illegal access " + host.getResource(), e);
        } catch (final SecurityException e) {
            s_logger.warn("Security error on " + host.getResource(), e);
        } catch (final NoSuchMethodException e) {
            s_logger.warn("NoSuchMethodException error on " + host.getResource(), e);
        } catch (final IllegalArgumentException e) {
            s_logger.warn("IllegalArgumentException error on " + host.getResource(), e);
        } catch (final InvocationTargetException e) {
            s_logger.warn("InvocationTargetException error on " + host.getResource(), e);
        }

        if (resource != null) {
            _hostDao.loadDetails(host);

            final HashMap<String, Object> params = new HashMap<String, Object>(host.getDetails().size() + 5);
            params.putAll(host.getDetails());

            params.put("guid", host.getGuid());
            params.put("zone", Long.toString(host.getDataCenterId()));
            if (host.getPodId() != null) {
                params.put("pod", Long.toString(host.getPodId()));
            }
            if (host.getClusterId() != null) {
                params.put("cluster", Long.toString(host.getClusterId()));
                String guid = null;
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
                s_logger.warn("Unable to configure resource due to " + e.getMessage());
                return null;
            }

            if (!resource.start()) {
                s_logger.warn("Unable to start the resource");
                return null;
            }
        }
        return resource;
    }

    @Override
    public void rescan() {
    }

    protected boolean loadDirectlyConnectedHost(final HostVO host, final boolean forRebalance) {
        boolean initialized = false;
        ServerResource resource = null;
        try {
            // load the respective discoverer
            final Discoverer discoverer = _resourceMgr.getMatchingDiscover(host.getHypervisorType());
            if (discoverer == null) {
                s_logger.info("Could not to find a Discoverer to load the resource: " + host.getId() + " for hypervisor type: " + host.getHypervisorType());
                resource = loadResourcesWithoutHypervisor(host);
            } else {
                resource = discoverer.reloadResource(host);
            }

            if (resource == null) {
                s_logger.warn("Unable to load the resource: " + host.getId());
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
            final Host h = _resourceMgr.createHostAndAgent(host.getId(), resource, host.getDetails(), false, null, true);
            tapLoadingAgents(host.getId(), TapAgentsAction.Del);

            return h == null ? false : true;
        } else {
            _executor.execute(new SimulateStartTask(host.getId(), resource, host.getDetails()));
            return true;
        }
    }

    protected AgentAttache createAttacheForDirectConnect(final Host host, final ServerResource resource) throws ConnectionException {
        s_logger.debug("create DirectAgentAttache for " + host.getId());
        final DirectAgentAttache attache = new DirectAgentAttache(this, host.getId(), host.getName(), resource, host.isInMaintenanceStates());

        AgentAttache old = null;
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

        s_logger.info("Disconnecting agents: " + _agents.size());
        synchronized (_agents) {
            for (final AgentAttache agent : _agents.values()) {
                final HostVO host = _hostDao.findById(agent.getId());
                if (host == null) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Cant not find host " + agent.getId());
                    }
                } else {
                    if (!agent.forForward()) {
                        agentStatusTransitTo(host, Event.ManagementServerDown, _nodeId);
                    }
                }
            }
        }

        _connectExecutor.shutdownNow();
        _monitorExecutor.shutdownNow();
        return true;
    }

    protected boolean handleDisconnectWithoutInvestigation(final AgentAttache attache, final Status.Event event, final boolean transitState, final boolean removeAgent) {
        final long hostId = attache.getId();

        s_logger.info("Host " + hostId + " is disconnecting with event " + event);
        Status nextStatus = null;
        final HostVO host = _hostDao.findById(hostId);
        if (host == null) {
            s_logger.warn("Can't find host with " + hostId);
            nextStatus = Status.Removed;
        } else {
            final Status currentStatus = host.getStatus();
            if (currentStatus == Status.Down || currentStatus == Status.Alert || currentStatus == Status.Removed) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Host " + hostId + " is already " + currentStatus);
                }
                nextStatus = currentStatus;
            } else {
                try {
                    nextStatus = currentStatus.getNextStatus(event);
                } catch (final NoTransitionException e) {
                    final String err = "Cannot find next status for " + event + " as current status is " + currentStatus + " for agent " + hostId;
                    s_logger.debug(err);
                    throw new CloudRuntimeException(err);
                }

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("The next status of agent " + hostId + "is " + nextStatus + ", current status is " + currentStatus);
                }
            }
            caService.purgeHostCertificate(host);
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Deregistering link for " + hostId + " with state " + nextStatus);
        }

        removeAgent(attache, nextStatus);
        // update the DB
        if (host != null && transitState) {
            disconnectAgent(host, event, _nodeId);
        }

        return true;
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
                s_logger.debug("Caught exception while getting agent's next status", ne);
            }

            if (nextStatus == Status.Alert) {
                /* OK, we are going to the bad status, let's see what happened */
                s_logger.info("Investigating why host " + hostId + " has disconnected with event " + event);

                Status determinedState = investigate(attache);
                // if state cannot be determined do nothing and bail out
                if (determinedState == null) {
                    if ((System.currentTimeMillis() >> 10) - host.getLastPinged() > AlertWait.value()) {
                        s_logger.warn("Agent " + hostId + " state cannot be determined for more than " + AlertWait + "(" + AlertWait.value() + ") seconds, will go to Alert state");
                        determinedState = Status.Alert;
                    } else {
                        s_logger.warn("Agent " + hostId + " state cannot be determined, do nothing");
                        return false;
                    }
                }

                final Status currentStatus = host.getStatus();
                s_logger.info("The agent from host " + hostId + " state determined is " + determinedState);

                if (determinedState == Status.Down) {
                    final String message = "Host is down: " + host.getId() + "-" + host.getName() + ". Starting HA on the VMs";
                    s_logger.error(message);
                    if (host.getType() != Host.Type.SecondaryStorage && host.getType() != Host.Type.ConsoleProxy) {
                        _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, host.getDataCenterId(), host.getPodId(), "Host down, " + host.getId(), message);
                    }
                    event = Status.Event.HostDown;
                } else if (determinedState == Status.Up) {
                    /* Got ping response from host, bring it back */
                    s_logger.info("Agent is determined to be up and running");
                    agentStatusTransitTo(host, Status.Event.Ping, _nodeId);
                    return false;
                } else if (determinedState == Status.Disconnected) {
                    s_logger.warn("Agent is disconnected but the host is still up: " + host.getId() + "-" + host.getName() +
                            '-' + host.getResourceState());
                    if (currentStatus == Status.Disconnected ||
                            (currentStatus == Status.Up && host.getResourceState() == ResourceState.PrepareForMaintenance)) {
                        if ((System.currentTimeMillis() >> 10) - host.getLastPinged() > AlertWait.value()) {
                            s_logger.warn("Host " + host.getId() + " has been disconnected past the wait time it should be disconnected.");
                            event = Status.Event.WaitedTooLong;
                        } else {
                            s_logger.debug("Host " + host.getId() + " has been determined to be disconnected but it hasn't passed the wait time yet.");
                            return false;
                        }
                    } else if (currentStatus == Status.Up) {
                        final DataCenterVO dcVO = _dcDao.findById(host.getDataCenterId());
                        final HostPodVO podVO = _podDao.findById(host.getPodId());
                        final String hostDesc = "name: " + host.getName() + " (id:" + host.getId() + "), availability zone: " + dcVO.getName() + ", pod: " + podVO.getName();
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
                    final String hostDesc = "name: " + host.getName() + " (id:" + host.getId() + "), availability zone: " + dcVO.getName() + ", pod: " + podName;
                    _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, host.getDataCenterId(), host.getPodId(), "Host in ALERT state, " + hostDesc,
                            "In availability zone " + host.getDataCenterId() + ", host is in alert state: " + host.getId() + "-" + host.getName());
                }
            } else {
                s_logger.debug("The next status of agent " + host.getId() + " is not Alert, no need to investigate what happened");
            }
        }
        handleDisconnectWithoutInvestigation(attache, event, true, true);
        host = _hostDao.findById(hostId); // Maybe the host magically reappeared?
        if (host != null && host.getStatus() == Status.Down) {
            _haMgr.scheduleRestartForVmsOnHost(host, true);
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
                if (_investigate == true) {
                    handleDisconnectWithInvestigation(_attache, _event);
                } else {
                    handleDisconnectWithoutInvestigation(_attache, _event, true, false);
                }
            } catch (final Exception e) {
                s_logger.error("Exception caught while handling disconnect: ", e);
            }
        }
    }

    @Override
    public Answer easySend(final Long hostId, final Command cmd) {
        try {
            final Host h = _hostDao.findById(hostId);
            if (h == null || h.getRemoved() != null) {
                s_logger.debug("Host with id " + hostId + " doesn't exist");
                return null;
            }
            final Status status = h.getStatus();
            if (!status.equals(Status.Up) && !status.equals(Status.Connecting)) {
                s_logger.debug("Can not send command " + cmd + " due to Host " + hostId + " is not up");
                return null;
            }
            final Answer answer = send(hostId, cmd);
            if (answer == null) {
                s_logger.warn("send returns null answer");
                return null;
            }

            if (s_logger.isDebugEnabled() && answer.getDetails() != null) {
                s_logger.debug("Details from executing " + cmd.getClass() + ": " + answer.getDetails());
            }

            return answer;

        } catch (final AgentUnavailableException e) {
            s_logger.warn(e.getMessage());
            return null;
        } catch (final OperationTimedoutException e) {
            s_logger.warn("Operation timed out: " + e.getMessage());
            return null;
        } catch (final Exception e) {
            s_logger.warn("Exception while sending", e);
            return null;
        }
    }

    @Override
    public Answer[] send(final Long hostId, final Commands cmds) throws AgentUnavailableException, OperationTimedoutException {
        int wait = 0;
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
            throw new CloudRuntimeException("Host has already been removed: " + hostId);
        }

        if (host.getStatus() == Status.Disconnected) {
            s_logger.debug("Host is already disconnected, no work to be done: " + hostId);
            return;
        }

        if (host.getStatus() != Status.Up && host.getStatus() != Status.Alert && host.getStatus() != Status.Rebalancing) {
            throw new CloudRuntimeException("Unable to disconnect host because it is not in the correct state: host=" + hostId + "; Status=" + host.getStatus());
        }

        AgentAttache attache = findAttache(hostId);
        if (attache == null) {
            throw new CloudRuntimeException("Unable to disconnect host because it is not connected to this server: " + hostId);
        }
        disconnectWithoutInvestigation(attache, Event.ShutdownRequested);
    }

    @Override
    public void notifyMonitorsOfHostAboutToBeRemoved(long hostId) {
        for (final Pair<Integer, Listener> monitor : _hostMonitors) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Sending host about to be removed to listener: " + monitor.second().getClass().getSimpleName());
            }

            monitor.second().processHostAboutToBeRemoved(hostId);
        }
    }

    @Override
    public void notifyMonitorsOfRemovedHost(long hostId, long clusterId) {
        for (final Pair<Integer, Listener> monitor : _hostMonitors) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Sending host removed to listener: " + monitor.second().getClass().getSimpleName());
            }

            monitor.second().processHostRemoved(hostId, clusterId);
        }
    }

    public boolean executeUserRequest(final long hostId, final Event event) throws AgentUnavailableException {
        if (event == Event.AgentDisconnected) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Received agent disconnect event for host " + hostId);
            }
            AgentAttache attache = null;
            attache = findAttache(hostId);
            if (attache != null) {
                handleDisconnectWithoutInvestigation(attache, Event.AgentDisconnected, true, true);
            }
            return true;
        }
        if (event == Event.ShutdownRequested) {
            try {
                reconnect(hostId);
            } catch (CloudRuntimeException e) {
                s_logger.debug("Error on shutdown request for hostID: " + hostId, e);
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

    protected AgentAttache createAttacheForConnect(final HostVO host, final Link link) throws ConnectionException {
        s_logger.debug("create ConnectedAgentAttache for " + host.getId());
        final AgentAttache attache = new ConnectedAgentAttache(this, host.getId(), host.getName(), link, host.isInMaintenanceStates());
        link.attach(attache);

        AgentAttache old = null;
        synchronized (_agents) {
            old = _agents.put(host.getId(), attache);
        }
        if (old != null) {
            old.disconnect(Status.Removed);
        }

        return attache;
    }

    private AgentAttache handleConnectedAgent(final Link link, final StartupCommand[] startup, final Request request) {
        AgentAttache attache = null;
        ReadyCommand ready = null;
        try {
            final List<String> agentMSHostList = new ArrayList<>();
            String lbAlgorithm = null;
            if (startup != null && startup.length > 0) {
                final String agentMSHosts = startup[0].getMsHostList();
                if (StringUtils.isNotEmpty(agentMSHosts)) {
                    String[] msHosts = agentMSHosts.split("@");
                    if (msHosts.length > 1) {
                        lbAlgorithm = msHosts[1];
                    }
                    agentMSHostList.addAll(Arrays.asList(msHosts[0].split(",")));
                }
            }

            final HostVO host = _resourceMgr.createHostVOForConnectedAgent(startup);
            if (host != null) {
                ready = new ReadyCommand(host.getDataCenterId(), host.getId(), NumbersUtil.enableHumanReadableSizes);

                if (!indirectAgentLB.compareManagementServerList(host.getId(), host.getDataCenterId(), agentMSHostList, lbAlgorithm)) {
                    final List<String> newMSList = indirectAgentLB.getManagementServerList(host.getId(), host.getDataCenterId(), null);
                    ready.setMsHostList(newMSList);
                    ready.setLbAlgorithm(indirectAgentLB.getLBAlgorithmName());
                    ready.setLbCheckInterval(indirectAgentLB.getLBPreferredHostCheckInterval(host.getClusterId()));
                    s_logger.debug("Agent's management server host list is not up to date, sending list update:" + newMSList);
                }

                attache = createAttacheForConnect(host, link);
                attache = notifyMonitorsOfConnection(attache, startup, false);
            }
        } catch (final Exception e) {
            s_logger.debug("Failed to handle host connection: ", e);
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
            s_logger.debug("Failed to send ready command:" + e.toString());
        }
        return attache;
    }

    protected class SimulateStartTask extends ManagedContextRunnable {
        ServerResource resource;
        Map<String, String> details;
        long id;

        public SimulateStartTask(final long id, final ServerResource resource, final Map<String, String> details) {
            this.id = id;
            this.resource = resource;
            this.details = details;
        }

        @Override
        protected void runInContext() {
            try {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Simulating start for resource " + resource.getName() + " id " + id);
                }

                if (tapLoadingAgents(id, TapAgentsAction.Add)) {
                    try {
                        final AgentAttache agentattache = findAttache(id);
                        if (agentattache == null) {
                            s_logger.debug("Creating agent for host " + id);
                            _resourceMgr.createHostAndAgent(id, resource, details, false, null, false);
                            s_logger.debug("Completed creating agent for host " + id);
                        } else {
                            s_logger.debug("Agent already created in another thread for host " + id + ", ignore this");
                        }
                    } finally {
                        tapLoadingAgents(id, TapAgentsAction.Del);
                    }
                } else {
                    s_logger.debug("Agent creation already getting processed in another thread for host " + id + ", ignore this");
                }
            } catch (final Exception e) {
                s_logger.warn("Unable to simulate start on resource " + id + " name " + resource.getName(), e);
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

            final AgentAttache attache = handleConnectedAgent(_link, startups, _request);
            if (attache == null) {
                s_logger.warn("Unable to create attache for agent: " + _request);
            }
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
                answers[i] = new StartupAnswer((StartupCommand) cmds[i], 0, mgmtServiceConf.getPingInterval());
                break;
            }
        }
        Response response = null;
        response = new Response(request, answers[0], _nodeId, -1);
        try {
            link.send(response.toBytes());
        } catch (final ClosedChannelException e) {
            s_logger.debug("Failed to send startupanswer: " + e.toString());
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
                s_logger.error(String.format("Unable to find host with ID: %s", hostId));
                return;
            }
            if (!BooleanUtils.toBoolean(EnableKVMAutoEnableDisable.valueIn(host.getClusterId()))) {
                s_logger.debug(String.format("%s is disabled for the cluster %s, cannot process the health check result " +
                        "received for the host %s", EnableKVMAutoEnableDisable.key(), host.getClusterId(), host.getName()));
                return;
            }

            ResourceState.Event resourceEvent = hostHealthCheckResult ? ResourceState.Event.Enable : ResourceState.Event.Disable;

            try {
                s_logger.info(String.format("Host health check %s, auto %s KVM host: %s",
                        hostHealthCheckResult ? "succeeds" : "fails",
                        hostHealthCheckResult ? "enabling" : "disabling",
                        host.getName()));
                _resourceMgr.autoUpdateHostAllocationState(hostId, resourceEvent);
            } catch (NoTransitionException e) {
                s_logger.error(String.format("Cannot Auto %s host: %s", resourceEvent, host.getName()), e);
            }
        }

        private void processStartupRoutingCommand(StartupRoutingCommand startup, long hostId) {
            if (startup == null) {
                s_logger.error("Empty StartupRoutingCommand received");
                return;
            }
            Boolean hostHealthCheckResult = startup.getHostHealthCheckResult();
            processHostHealthCheckResult(hostHealthCheckResult, hostId);
        }

        private void processPingRoutingCommand(PingRoutingCommand pingRoutingCommand, long hostId) {
            if (pingRoutingCommand == null) {
                s_logger.error("Empty PingRoutingCommand received");
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
                    s_logger.warn("Throwing away a request because it came through as the first command on a connect: " + request);
                } else {
                    // submit the task for execution
                    request.logD("Scheduling the first command ");
                    connectAgent(link, cmds, request);
                }
                return;
            }

            final long hostId = attache.getId();
            final String hostName = attache.getName();

            if (s_logger.isDebugEnabled()) {
                if (cmd instanceof PingRoutingCommand) {
                    logD = false;
                    s_logger.debug("Ping from Routing host " + hostId + "(" + hostName + ")");
                    s_logger.trace("SeqA " + hostId + "-" + request.getSequence() + ": Processing " + request);
                } else if (cmd instanceof PingCommand) {
                    logD = false;
                    s_logger.debug("Ping from " + hostId + "(" + hostName + ")");
                    s_logger.trace("SeqA " + hostId + "-" + request.getSequence() + ": Processing " + request);
                } else {
                    s_logger.debug("SeqA " + hostId + "-" + request.getSequence() + ": Processing " + request);
                }
            }

            final Answer[] answers = new Answer[cmds.length];
            for (int i = 0; i < cmds.length; i++) {
                cmd = cmds[i];
                Answer answer = null;
                try {
                    if (cmd instanceof StartupRoutingCommand) {
                        final StartupRoutingCommand startup = (StartupRoutingCommand) cmd;
                        processStartupRoutingCommand(startup, hostId);
                        answer = new StartupAnswer(startup, attache.getId(), mgmtServiceConf.getPingInterval());
                    } else if (cmd instanceof StartupProxyCommand) {
                        final StartupProxyCommand startup = (StartupProxyCommand) cmd;
                        answer = new StartupAnswer(startup, attache.getId(), mgmtServiceConf.getPingInterval());
                    } else if (cmd instanceof StartupSecondaryStorageCommand) {
                        final StartupSecondaryStorageCommand startup = (StartupSecondaryStorageCommand) cmd;
                        answer = new StartupAnswer(startup, attache.getId(), mgmtServiceConf.getPingInterval());
                    } else if (cmd instanceof StartupStorageCommand) {
                        final StartupStorageCommand startup = (StartupStorageCommand) cmd;
                        answer = new StartupAnswer(startup, attache.getId(), mgmtServiceConf.getPingInterval());
                    } else if (cmd instanceof ShutdownCommand) {
                        final ShutdownCommand shutdown = (ShutdownCommand)cmd;
                        final String reason = shutdown.getReason();
                        s_logger.info("Host " + attache.getId() + " has informed us that it is shutting down with reason " + reason + " and detail " + shutdown.getDetail());
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

                            // if the router is sending a ping, verify the
                            // gateway was pingable
                            if (cmd instanceof PingRoutingCommand) {
                                processPingRoutingCommand((PingRoutingCommand) cmd, hostId);
                                final boolean gatewayAccessible = ((PingRoutingCommand)cmd).isGatewayAccessible();
                                final HostVO host = _hostDao.findById(Long.valueOf(cmdHostId));

                                if (host != null) {
                                    if (!gatewayAccessible) {
                                        // alert that host lost connection to
                                        // gateway (cannot ping the default route)
                                        final DataCenterVO dcVO = _dcDao.findById(host.getDataCenterId());
                                        final HostPodVO podVO = _podDao.findById(host.getPodId());
                                        final String hostDesc = "name: " + host.getName() + " (id:" + host.getId() + "), availability zone: " + dcVO.getName() + ", pod: " + podVO.getName();

                                        _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_ROUTING, host.getDataCenterId(), host.getPodId(), "Host lost connection to gateway, " + hostDesc,
                                                "Host [" + hostDesc + "] lost connection to gateway (default route) and is possibly having network connection issues.");
                                    } else {
                                        _alertMgr.clearAlert(AlertManager.AlertType.ALERT_TYPE_ROUTING, host.getDataCenterId(), host.getPodId());
                                    }
                                } else {
                                    s_logger.debug("Not processing " + PingRoutingCommand.class.getSimpleName() + " for agent id=" + cmdHostId + "; can't find the host in the DB");
                                }
                            }
                            answer = new PingAnswer((PingCommand)cmd);
                        } else if (cmd instanceof ReadyAnswer) {
                            final HostVO host = _hostDao.findById(attache.getId());
                            if (host == null) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Cant not find host " + attache.getId());
                                }
                            }
                            answer = new Answer(cmd);
                        } else {
                            answer = new Answer(cmd);
                        }
                    }
                } catch (final Throwable th) {
                    s_logger.warn("Caught: ", th);
                    answer = new Answer(cmd, false, th.getMessage());
                }
                answers[i] = answer;
            }

            final Response response = new Response(request, answers, _nodeId, attache.getId());
            if (s_logger.isDebugEnabled()) {
                if (logD) {
                    s_logger.debug("SeqA " + attache.getId() + "-" + response.getSequence() + ": Sending " + response);
                } else {
                    s_logger.trace("SeqA " + attache.getId() + "-" + response.getSequence() + ": Sending " + response);
                }
            }
            try {
                link.send(response.toBytes());
            } catch (final ClosedChannelException e) {
                s_logger.warn("Unable to send response because connection is closed: " + response);
            }
        }

        protected void processResponse(final Link link, final Response response) {
            final AgentAttache attache = (AgentAttache)link.attachment();
            if (attache == null) {
                s_logger.warn("Unable to process: " + response);
            } else if (!attache.processAnswers(response.getSequence(), response)) {
                s_logger.info("Host " + attache.getId() + " - Seq " + response.getSequence() + ": Response is not processed: " + response);
            }
        }

        @Override
        protected void doTask(final Task task) throws TaskExecutionException {
            final TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            try {
                final Type type = task.getType();
                if (type == Task.Type.DATA) {
                    final byte[] data = task.getData();
                    try {
                        final Request event = Request.parse(data);
                        if (event instanceof Response) {
                            processResponse(task.getLink(), (Response)event);
                        } else {
                            processRequest(task.getLink(), event);
                        }
                    } catch (final UnsupportedVersionException e) {
                        s_logger.warn(e.getMessage());
                        // upgradeAgent(task.getLink(), data, e.getReason());
                    } catch (final ClassNotFoundException e) {
                        final String message = String.format("Exception occurred when executing tasks! Error '%s'", e.getMessage());
                        s_logger.error(message);
                        throw new TaskExecutionException(message, e);
                    }
                } else if (type == Task.Type.CONNECT) {
                } else if (type == Task.Type.DISCONNECT) {
                    final Link link = task.getLink();
                    final AgentAttache attache = (AgentAttache)link.attachment();
                    if (attache != null) {
                        disconnectWithInvestigation(attache, Event.AgentDisconnected);
                    } else {
                        s_logger.info("Connection from " + link.getIpAddress() + " closed but no cleanup was done.");
                        link.close();
                        link.terminated();
                    }
                }
            } finally {
                txn.close();
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
        try {
            _agentStatusLock.lock();
            if (s_logger.isDebugEnabled()) {
                final ResourceState state = host.getResourceState();
                final StringBuilder msg = new StringBuilder("Transition:");
                msg.append("[Resource state = ").append(state);
                msg.append(", Agent event = ").append(e.toString());
                msg.append(", Host id = ").append(host.getId()).append(", name = " + host.getName()).append("]");
                s_logger.debug(msg);
            }

            host.setManagementServerId(msId);
            try {
                return _statusStateMachine.transitTo(host, e, host.getId(), _hostDao);
            } catch (final NoTransitionException e1) {
                s_logger.debug("Cannot transit agent status with event " + e + " for host " + host.getId() + ", name=" + host.getName() + ", management server id is " + msId);
                throw new CloudRuntimeException("Cannot transit agent status with event " + e + " for host " + host.getId() + ", management server id is " + msId + "," + e1.getMessage());
            }
        } finally {
            _agentStatusLock.unlock();
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
            s_logger.warn("Can't find the host " + hostId);
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

    private void disconnectInternal(final long hostId, final Status.Event event, final boolean invstigate) {
        final AgentAttache attache = findAttache(hostId);

        if (attache != null) {
            if (!invstigate) {
                disconnectWithoutInvestigation(attache, event);
            } else {
                disconnectWithInvestigation(attache, event);
            }
        } else {
            /* Agent is still in connecting process, don't allow to disconnect right away */
            if (tapLoadingAgents(hostId, TapAgentsAction.Contains)) {
                s_logger.info("Host " + hostId + " is being loaded so no disconnects needed.");
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
            answers[i] = new StartupAnswer(cmds[i], attache.getId(), mgmtServiceConf.getPingInterval());
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
            s_logger.info("PingMap for agent: " + agentId + " will not be updated because agent is no longer in the PingMap");
        }
    }

    protected class MonitorTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            s_logger.trace("Agent Monitor is started.");

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
                            s_logger.debug("Ping timeout but agent " + agentId + " is in resource state of " + resourceState + ", so no investigation");
                            disconnectWithoutInvestigation(agentId, Event.ShutdownRequested);
                        } else {
                            final HostVO host = _hostDao.findById(agentId);
                            if (host != null
                                    && (host.getType() == Host.Type.ConsoleProxy || host.getType() == Host.Type.SecondaryStorageVM || host.getType() == Host.Type.SecondaryStorageCmdExecutor)) {

                                s_logger.warn("Disconnect agent for CPVM/SSVM due to physical connection close. host: " + host.getId());
                                disconnectWithoutInvestigation(agentId, Event.ShutdownRequested);
                            } else {
                                s_logger.debug("Ping timeout for agent " + agentId + ", do invstigation");
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
                s_logger.error("Caught the following exception: ", th);
            }

            s_logger.trace("Agent Monitor is leaving the building!");
        }

        protected List<Long> findAgentsBehindOnPing() {
            final List<Long> agentsBehind = new ArrayList<Long>();
            final long cutoffTime = InaccurateClock.getTimeInSeconds() - mgmtServiceConf.getTimeout();
            for (final Map.Entry<Long, Long> entry : _pingMap.entrySet()) {
                if (entry.getValue() < cutoffTime) {
                    agentsBehind.add(entry.getKey());
                }
            }

            if (agentsBehind.size() > 0) {
                s_logger.info("Found the following agents behind on ping: " + agentsBehind);
            }

            return agentsBehind;
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
                DirectAgentPoolSize, DirectAgentThreadCap, EnableKVMAutoEnableDisable };
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
            if (cmd instanceof StartupRoutingCommand) {
                if (((StartupRoutingCommand)cmd).getHypervisorType() == HypervisorType.KVM || ((StartupRoutingCommand)cmd).getHypervisorType() == HypervisorType.LXC) {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put(Config.RouterAggregationCommandEachTimeout.toString(), _configDao.getValue(Config.RouterAggregationCommandEachTimeout.toString()));
                    params.put(Config.MigrateWait.toString(), _configDao.getValue(Config.MigrateWait.toString()));
                    params.put(NetworkOrchestrationService.TUNGSTEN_ENABLED.key(), String.valueOf(NetworkOrchestrationService.TUNGSTEN_ENABLED.valueIn(host.getDataCenterId())));

                    try {
                        SetHostParamsCommand cmds = new SetHostParamsCommand(params);
                        Commands c = new Commands(cmds);
                        send(host.getId(), c, this);
                    } catch (AgentUnavailableException e) {
                        s_logger.debug("Failed to send host params on host: " + host.getId());
                    }
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
        Map<Long, List<Long>> hostsByZone = new HashMap<Long, List<Long>>();
        for (HostVO host : allHosts) {
            if (host.getHypervisorType() == HypervisorType.KVM || host.getHypervisorType() == HypervisorType.LXC) {
                Long zoneId = host.getDataCenterId();
                List<Long> hostIds = hostsByZone.get(zoneId);
                if (hostIds == null) {
                    hostIds = new ArrayList<Long>();
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
                    s_logger.error("Error sending parameters to agent " + hostId);
                }
            }
        }
    }

    @Override
    public void propagateChangeToAgents(Map<String, String> params) {
        if (params != null && ! params.isEmpty()) {
            s_logger.debug("Propagating changes on host parameters to the agents");
            Map<Long, List<Long>> hostsPerZone = getHostsPerZone();
            sendCommandToAgents(hostsPerZone, params);
        }
    }
}
