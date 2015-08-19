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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.apache.log4j.Logger;

import com.google.gson.Gson;

import org.apache.cloudstack.framework.config.ConfigDepot;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.managed.context.ManagedContextTimerTask;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.cloudstack.utils.security.SSLUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CancelCommand;
import com.cloud.agent.api.ChangeAgentAnswer;
import com.cloud.agent.api.ChangeAgentCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PropagateResourceEventCommand;
import com.cloud.agent.api.ScheduleHostScanTaskCommand;
import com.cloud.agent.api.TransferAgentCommand;
import com.cloud.agent.transport.Request;
import com.cloud.agent.transport.Request.Version;
import com.cloud.agent.transport.Response;
import com.cloud.cluster.ClusterManager;
import com.cloud.cluster.ClusterManagerListener;
import com.cloud.cluster.ClusterServicePdu;
import com.cloud.cluster.ClusteredAgentRebalanceService;
import com.cloud.cluster.ManagementServerHost;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.agentlb.AgentLoadBalancerPlanner;
import com.cloud.cluster.agentlb.HostTransferMapVO;
import com.cloud.cluster.agentlb.HostTransferMapVO.HostTransferState;
import com.cloud.cluster.agentlb.dao.HostTransferMapDao;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.UnsupportedVersionException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.Status.Event;
import com.cloud.resource.ServerResource;
import com.cloud.serializer.GsonHelper;
import com.cloud.utils.DateUtil;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.nio.Link;
import com.cloud.utils.nio.Task;

@Local(value = {AgentManager.class, ClusteredAgentRebalanceService.class})
public class ClusteredAgentManagerImpl extends AgentManagerImpl implements ClusterManagerListener, ClusteredAgentRebalanceService {
    final static Logger s_logger = Logger.getLogger(ClusteredAgentManagerImpl.class);
    private static final ScheduledExecutorService s_transferExecutor = Executors.newScheduledThreadPool(2, new NamedThreadFactory("Cluster-AgentRebalancingExecutor"));
    private final long rebalanceTimeOut = 300000; // 5 mins - after this time remove the agent from the transfer list

    public final static long STARTUP_DELAY = 5000;
    public final static long SCAN_INTERVAL = 90000; // 90 seconds, it takes 60 sec for xenserver to fail login
    public final static int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 5; // 5 seconds
    protected Set<Long> _agentToTransferIds = new HashSet<Long>();
    Gson _gson;
    protected HashMap<String, SocketChannel> _peers;
    protected HashMap<String, SSLEngine> _sslEngines;
    private final Timer _timer = new Timer("ClusteredAgentManager Timer");
    boolean _agentLbHappened = false;

    @Inject
    protected ClusterManager _clusterMgr = null;
    @Inject
    protected ManagementServerHostDao _mshostDao;
    @Inject
    protected HostTransferMapDao _hostTransferDao;
    @Inject
    protected List<AgentLoadBalancerPlanner> _lbPlanners;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    ConfigDepot _configDepot;

    protected ClusteredAgentManagerImpl() {
        super();
    }

    protected final ConfigKey<Boolean> EnableLB = new ConfigKey<Boolean>(Boolean.class, "agent.lb.enabled", "Advanced", "false",
            "Enable agent load balancing between management server nodes", true);
    protected final ConfigKey<Double> ConnectedAgentThreshold = new ConfigKey<Double>(Double.class, "agent.load.threshold", "Advanced", "0.7",
            "What percentage of the agents can be held by one management server before load balancing happens", true);
    protected final ConfigKey<Integer> LoadSize = new ConfigKey<Integer>(Integer.class, "direct.agent.load.size", "Advanced", "16",
            "How many agents to connect to in each round", true);
    protected final ConfigKey<Integer> ScanInterval = new ConfigKey<Integer>(Integer.class, "direct.agent.scan.interval", "Advanced", "90",
            "Interval between scans to load agents", false, ConfigKey.Scope.Global, 1000);

    @Override
    public boolean configure(String name, Map<String, Object> xmlParams) throws ConfigurationException {
        _peers = new HashMap<String, SocketChannel>(7);
        _sslEngines = new HashMap<String, SSLEngine>(7);
        _nodeId = ManagementServerNode.getManagementServerId();

        s_logger.info("Configuring ClusterAgentManagerImpl. management server node id(msid): " + _nodeId);

        ClusteredAgentAttache.initialize(this);

        _clusterMgr.registerListener(this);
        _clusterMgr.registerDispatcher(new ClusterDispatcher());

        _gson = GsonHelper.getGson();

        return super.configure(name, xmlParams);
    }

    @Override
    public boolean start() {
        if (!super.start()) {
            return false;
        }
        _timer.schedule(new DirectAgentScanTimerTask(), STARTUP_DELAY, ScanInterval.value());
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Scheduled direct agent scan task to run at an interval of " + ScanInterval.value() + " seconds");
        }

        // Schedule tasks for agent rebalancing
        if (isAgentRebalanceEnabled()) {
            s_transferExecutor.scheduleAtFixedRate(getAgentRebalanceScanTask(), 60000, 60000, TimeUnit.MILLISECONDS);
            s_transferExecutor.scheduleAtFixedRate(getTransferScanTask(), 60000, ClusteredAgentRebalanceService.DEFAULT_TRANSFER_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
        }

        return true;
    }

    public void scheduleHostScanTask() {
        _timer.schedule(new DirectAgentScanTimerTask(), 0);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Scheduled a direct agent scan task");
        }
    }

    private void runDirectAgentScanTimerTask() {
        scanDirectAgentToLoad();
    }

    private void scanDirectAgentToLoad() {
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Begin scanning directly connected hosts");
        }

        // for agents that are self-managed, threshold to be considered as disconnected after pingtimeout
        long cutSeconds = (System.currentTimeMillis() >> 10) - getTimeout();
        List<HostVO> hosts = _hostDao.findAndUpdateDirectAgentToLoad(cutSeconds, LoadSize.value().longValue(), _nodeId);
        List<HostVO> appliances = _hostDao.findAndUpdateApplianceToLoad(cutSeconds, _nodeId);

       if (hosts != null) {
            hosts.addAll(appliances);
            if (hosts.size() > 0) {
                s_logger.debug("Found " + hosts.size() + " unmanaged direct hosts, processing connect for them...");
                for (HostVO host : hosts) {
                    try {
                        AgentAttache agentattache = findAttache(host.getId());
                        if (agentattache != null) {
                            // already loaded, skip
                            if (agentattache.forForward()) {
                                if (s_logger.isInfoEnabled()) {
                                    s_logger.info(host + " is detected down, but we have a forward attache running, disconnect this one before launching the host");
                                }
                                removeAgent(agentattache, Status.Disconnected);
                            } else {
                                continue;
                            }
                        }

                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Loading directly connected host " + host.getId() + "(" + host.getName() + ")");
                        }
                        loadDirectlyConnectedHost(host, false);
                    } catch (Throwable e) {
                        s_logger.warn(" can not load directly connected host " + host.getId() + "(" + host.getName() + ") due to ", e);
                    }
                }
            }
        }
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("End scanning directly connected hosts");
        }
    }

    private class DirectAgentScanTimerTask extends ManagedContextTimerTask {
        @Override
        protected void runInContext() {
            try {
                runDirectAgentScanTimerTask();
            } catch (Throwable e) {
                s_logger.error("Unexpected exception " + e.getMessage(), e);
            }
        }
    }

    @Override
    public Task create(Task.Type type, Link link, byte[] data) {
        return new ClusteredAgentHandler(type, link, data);
    }

    protected AgentAttache createAttache(long id) {
        s_logger.debug("create forwarding ClusteredAgentAttache for " + id);
        HostVO host = _hostDao.findById(id);
        final AgentAttache attache = new ClusteredAgentAttache(this, id, host.getName());
        AgentAttache old = null;
        synchronized (_agents) {
            old = _agents.get(id);
            _agents.put(id, attache);
        }
        if (old != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Remove stale agent attache from current management server");
            }
            removeAgent(old, Status.Removed);
        }
        return attache;
    }

    @Override
    protected AgentAttache createAttacheForConnect(HostVO host, Link link) {
        s_logger.debug("create ClusteredAgentAttache for " + host.getId());
        final AgentAttache attache = new ClusteredAgentAttache(this, host.getId(), host.getName(), link, host.isInMaintenanceStates());
        link.attach(attache);
        AgentAttache old = null;
        synchronized (_agents) {
            old = _agents.get(host.getId());
            _agents.put(host.getId(), attache);
        }
        if (old != null) {
            old.disconnect(Status.Removed);
        }
        return attache;
    }

    @Override
    protected AgentAttache createAttacheForDirectConnect(Host host, ServerResource resource) {
        s_logger.debug("create ClusteredDirectAgentAttache for " + host.getId());
        final DirectAgentAttache attache = new ClusteredDirectAgentAttache(this, host.getId(), host.getName(), _nodeId, resource, host.isInMaintenanceStates());
        AgentAttache old = null;
        synchronized (_agents) {
            old = _agents.get(host.getId());
            _agents.put(host.getId(), attache);
        }
        if (old != null) {
            old.disconnect(Status.Removed);
        }
        return attache;
    }

    @Override
    protected boolean handleDisconnectWithoutInvestigation(AgentAttache attache, Status.Event event, boolean transitState, boolean removeAgent) {
        return handleDisconnect(attache, event, false, true, removeAgent);
    }

    @Override
    protected boolean handleDisconnectWithInvestigation(AgentAttache attache, Status.Event event) {
        return handleDisconnect(attache, event, true, true, true);
    }

    protected boolean handleDisconnect(AgentAttache agent, Status.Event event, boolean investigate, boolean broadcast, boolean removeAgent) {
        boolean res;
        if (!investigate) {
            res = super.handleDisconnectWithoutInvestigation(agent, event, true, removeAgent);
        } else {
            res = super.handleDisconnectWithInvestigation(agent, event);
        }

        if (res) {
            if (broadcast) {
                notifyNodesInCluster(agent);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean executeUserRequest(long hostId, Event event) throws AgentUnavailableException {
        if (event == Event.AgentDisconnected) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Received agent disconnect event for host " + hostId);
            }
            AgentAttache attache = findAttache(hostId);
            if (attache != null) {
                // don't process disconnect if the host is being rebalanced
                if (isAgentRebalanceEnabled()) {
                    HostTransferMapVO transferVO = _hostTransferDao.findById(hostId);
                    if (transferVO != null) {
                        if (transferVO.getFutureOwner() == _nodeId && transferVO.getState() == HostTransferState.TransferStarted) {
                            s_logger.debug("Not processing " + Event.AgentDisconnected + " event for the host id=" + hostId + " as the host is being connected to " +
                                    _nodeId);
                            return true;
                        }
                    }
                }

                // don't process disconnect if the disconnect came for the host via delayed cluster notification,
                // but the host has already reconnected to the current management server
                if (!attache.forForward()) {
                    s_logger.debug("Not processing " + Event.AgentDisconnected + " event for the host id=" + hostId +
                            " as the host is directly connected to the current management server " + _nodeId);
                    return true;
                }

                return super.handleDisconnectWithoutInvestigation(attache, Event.AgentDisconnected, false, true);
            }

            return true;
        } else {
            return super.executeUserRequest(hostId, event);
        }
    }

    @Override
    public boolean reconnect(final long hostId) {
        Boolean result;
        try {
            result = propagateAgentEvent(hostId, Event.ShutdownRequested);
            if (result != null) {
                return result;
            }
        } catch (AgentUnavailableException e) {
            s_logger.debug("cannot propagate agent reconnect because agent is not available", e);
            return false;
        }

        return super.reconnect(hostId);
    }

    public void notifyNodesInCluster(AgentAttache attache) {
        s_logger.debug("Notifying other nodes of to disconnect");
        Command[] cmds = new Command[] {new ChangeAgentCommand(attache.getId(), Event.AgentDisconnected)};
        _clusterMgr.broadcast(attache.getId(), _gson.toJson(cmds));
    }

    // notifies MS peers to schedule a host scan task immediately, triggered during addHost operation
    public void notifyNodesInClusterToScheduleHostScanTask() {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Notifying other MS nodes to run host scan task");
        }
        Command[] cmds = new Command[] {new ScheduleHostScanTaskCommand()};
        _clusterMgr.broadcast(0, _gson.toJson(cmds));
    }

    protected static void logT(byte[] bytes, final String msg) {
        s_logger.trace("Seq " + Request.getAgentId(bytes) + "-" + Request.getSequence(bytes) + ": MgmtId " + Request.getManagementServerId(bytes) + ": " +
                (Request.isRequest(bytes) ? "Req: " : "Resp: ") + msg);
    }

    protected static void logD(byte[] bytes, final String msg) {
        s_logger.debug("Seq " + Request.getAgentId(bytes) + "-" + Request.getSequence(bytes) + ": MgmtId " + Request.getManagementServerId(bytes) + ": " +
                (Request.isRequest(bytes) ? "Req: " : "Resp: ") + msg);
    }

    protected static void logI(byte[] bytes, final String msg) {
        s_logger.info("Seq " + Request.getAgentId(bytes) + "-" + Request.getSequence(bytes) + ": MgmtId " + Request.getManagementServerId(bytes) + ": " +
                (Request.isRequest(bytes) ? "Req: " : "Resp: ") + msg);
    }

    public boolean routeToPeer(String peer, byte[] bytes) {
        int i = 0;
        SocketChannel ch = null;
        SSLEngine sslEngine = null;
        while (i++ < 5) {
            ch = connectToPeer(peer, ch);
            if (ch == null) {
                try {
                    logD(bytes, "Unable to route to peer: " + Request.parse(bytes).toString());
                } catch (ClassNotFoundException | UnsupportedVersionException e) {
                    // Request.parse thrown exception when we try to log it, log as much as we can
                    logD(bytes, "Unable to route to peer, and Request.parse further caught exception" + e.getMessage());
                }
                return false;
            }
            sslEngine = getSSLEngine(peer);
            if (sslEngine == null) {
                logD(bytes, "Unable to get SSLEngine of peer: " + peer);
                return false;
            }
            try {
                if (s_logger.isDebugEnabled()) {
                    logD(bytes, "Routing to peer");
                }
                Link.write(ch, new ByteBuffer[] {ByteBuffer.wrap(bytes)}, sslEngine);
                return true;
            } catch (IOException e) {
                try {
                    logI(bytes, "Unable to route to peer: " + Request.parse(bytes).toString() + " due to " + e.getMessage());
                } catch (ClassNotFoundException | UnsupportedVersionException ex) {
                    // Request.parse thrown exception when we try to log it, log as much as we can
                    logI(bytes, "Unable to route to peer due to" + e.getMessage()
                            + ". Also caught exception when parsing request: " + ex.getMessage());
                }
            }
        }
        return false;
    }

    public String findPeer(long hostId) {
        return getPeerName(hostId);
    }

    public SSLEngine getSSLEngine(String peerName) {
        return _sslEngines.get(peerName);
    }

    public void cancel(String peerName, long hostId, long sequence, String reason) {
        CancelCommand cancel = new CancelCommand(sequence, reason);
        Request req = new Request(hostId, _nodeId, cancel, true);
        req.setControl(true);
        routeToPeer(peerName, req.getBytes());
    }

    public void closePeer(String peerName) {
        synchronized (_peers) {
            SocketChannel ch = _peers.get(peerName);
            if (ch != null) {
                try {
                    ch.close();
                } catch (IOException e) {
                    s_logger.warn("Unable to close peer socket connection to " + peerName);
                }
            }
            _peers.remove(peerName);
            _sslEngines.remove(peerName);
        }
    }

    public SocketChannel connectToPeer(String peerName, SocketChannel prevCh) {
        synchronized (_peers) {
            SocketChannel ch = _peers.get(peerName);
            SSLEngine sslEngine = null;
            if (prevCh != null) {
                try {
                    prevCh.close();
                } catch (Exception e) {
                    s_logger.info("[ignored]"
                            + "failed to get close resource for previous channel Socket: " + e.getLocalizedMessage());
                }
            }
            if (ch == null || ch == prevCh) {
                ManagementServerHost ms = _clusterMgr.getPeer(peerName);
                if (ms == null) {
                    s_logger.info("Unable to find peer: " + peerName);
                    return null;
                }
                String ip = ms.getServiceIP();
                InetAddress addr;
                try {
                    addr = InetAddress.getByName(ip);
                } catch (UnknownHostException e) {
                    throw new CloudRuntimeException("Unable to resolve " + ip);
                }
                SocketChannel ch1 = null;
                try {
                    ch1 = SocketChannel.open(new InetSocketAddress(addr, Port.value()));
                    ch1.configureBlocking(true); // make sure we are working at blocking mode
                    ch1.socket().setKeepAlive(true);
                    ch1.socket().setSoTimeout(60 * 1000);
                    try {
                        SSLContext sslContext = Link.initSSLContext(true);
                        sslEngine = sslContext.createSSLEngine(ip, Port.value());
                        sslEngine.setUseClientMode(true);
                        sslEngine.setEnabledProtocols(SSLUtils.getSupportedProtocols(sslEngine.getEnabledProtocols()));

                        Link.doHandshake(ch1, sslEngine, true);
                        s_logger.info("SSL: Handshake done");
                    } catch (Exception e) {
                        ch1.close();
                        throw new IOException("SSL: Fail to init SSL! " + e);
                    }
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Connection to peer opened: " + peerName + ", ip: " + ip);
                    }
                    _peers.put(peerName, ch1);
                    _sslEngines.put(peerName, sslEngine);
                    return ch1;
                } catch (IOException e) {
                    try {
                        ch1.close();
                    } catch (IOException ex) {
                        s_logger.error("failed to close failed peer socket: " + ex);
                    }
                    s_logger.warn("Unable to connect to peer management server: " + peerName + ", ip: " + ip + " due to " + e.getMessage(), e);
                    return null;
                }
            }

            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Found open channel for peer: " + peerName);
            }
            return ch;
        }
    }

    public SocketChannel connectToPeer(long hostId, SocketChannel prevCh) {
        String peerName = getPeerName(hostId);
        if (peerName == null) {
            return null;
        }

        return connectToPeer(peerName, prevCh);
    }

    @Override
    protected AgentAttache getAttache(final Long hostId) throws AgentUnavailableException {
        assert (hostId != null) : "Who didn't check their id value?";
        HostVO host = _hostDao.findById(hostId);
        if (host == null) {
            throw new AgentUnavailableException("Can't find the host ", hostId);
        }

        AgentAttache agent = findAttache(hostId);
        if (agent == null || !agent.forForward()) {
            if (isHostOwnerSwitched(host)) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Host " + hostId + " has switched to another management server, need to update agent map with a forwarding agent attache");
                }
                agent = createAttache(hostId);
            }
        }
        if (agent == null) {
            AgentUnavailableException ex = new AgentUnavailableException("Host with specified id is not in the right state: " + host.getStatus(), hostId);
            ex.addProxyObject(_entityMgr.findById(Host.class, hostId).getUuid());
            throw ex;
        }

        return agent;
    }

    @Override
    public boolean stop() {
        if (_peers != null) {
            for (SocketChannel ch : _peers.values()) {
                try {
                    s_logger.info("Closing: " + ch.toString());
                    ch.close();
                } catch (IOException e) {
                    s_logger.info("[ignored] error on closing channel: " +ch.toString(), e);
                }
            }
        }
        _timer.cancel();

        // cancel all transfer tasks
        s_transferExecutor.shutdownNow();
        cleanupTransferMap(_nodeId);

        return super.stop();
    }

    @Override
    public void startDirectlyConnectedHosts() {
        // override and let it be dummy for purpose, we will scan and load direct agents periodically.
        // We may also pickup agents that have been left over from other crashed management server
    }

    public class ClusteredAgentHandler extends AgentHandler {

        public ClusteredAgentHandler(Task.Type type, Link link, byte[] data) {
            super(type, link, data);
        }

        @Override
        protected void doTask(final Task task) throws Exception {
            TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            try {
                if (task.getType() != Task.Type.DATA) {
                    super.doTask(task);
                    return;
                }

                final byte[] data = task.getData();
                Version ver = Request.getVersion(data);
                if (ver.ordinal() != Version.v1.ordinal() && ver.ordinal() != Version.v3.ordinal()) {
                    s_logger.warn("Wrong version for clustered agent request");
                    super.doTask(task);
                    return;
                }

                long hostId = Request.getAgentId(data);
                Link link = task.getLink();

                if (Request.fromServer(data)) {

                    AgentAttache agent = findAttache(hostId);

                    if (Request.isControl(data)) {
                        if (agent == null) {
                            logD(data, "No attache to process cancellation");
                            return;
                        }
                        Request req = Request.parse(data);
                        Command[] cmds = req.getCommands();
                        CancelCommand cancel = (CancelCommand)cmds[0];
                        if (s_logger.isDebugEnabled()) {
                            logD(data, "Cancel request received");
                        }
                        agent.cancel(cancel.getSequence());
                        final Long current = agent._currentSequence;
                        // if the request is the current request, always have to trigger sending next request in
// sequence,
                        // otherwise the agent queue will be blocked
                        if (req.executeInSequence() && (current != null && current == Request.getSequence(data))) {
                            agent.sendNext(Request.getSequence(data));
                        }
                        return;
                    }

                    try {
                        if (agent == null || agent.isClosed()) {
                            throw new AgentUnavailableException("Unable to route to agent ", hostId);
                        }

                        if (Request.isRequest(data) && Request.requiresSequentialExecution(data)) {
                            // route it to the agent.
                            // But we have the serialize the control commands here so we have
                            // to deserialize this and send it through the agent attache.
                            Request req = Request.parse(data);
                            agent.send(req, null);
                            return;
                        } else {
                            if (agent instanceof Routable) {
                                Routable cluster = (Routable)agent;
                                cluster.routeToAgent(data);
                            } else {
                                agent.send(Request.parse(data));
                            }
                            return;
                        }
                    } catch (AgentUnavailableException e) {
                        logD(data, e.getMessage());
                        cancel(Long.toString(Request.getManagementServerId(data)), hostId, Request.getSequence(data), e.getMessage());
                    }
                } else {

                    long mgmtId = Request.getManagementServerId(data);
                    if (mgmtId != -1 && mgmtId != _nodeId) {
                        routeToPeer(Long.toString(mgmtId), data);
                        if (Request.requiresSequentialExecution(data)) {
                            AgentAttache attache = (AgentAttache)link.attachment();
                            if (attache != null) {
                                attache.sendNext(Request.getSequence(data));
                            } else if (s_logger.isDebugEnabled()) {
                                logD(data, "No attache to process " + Request.parse(data).toString());
                            }
                        }
                        return;
                    } else {
                        if (Request.isRequest(data)) {
                            super.doTask(task);
                        } else {
                            // received an answer.
                            final Response response = Response.parse(data);
                            AgentAttache attache = findAttache(response.getAgentId());
                            if (attache == null) {
                                s_logger.info("SeqA " + response.getAgentId() + "-" + response.getSequence() + "Unable to find attache to forward " + response.toString());
                                return;
                            }
                            if (!attache.processAnswers(response.getSequence(), response)) {
                                s_logger.info("SeqA " + attache.getId() + "-" + response.getSequence() + ": Response is not processed: " + response.toString());
                            }
                        }
                        return;
                    }
                }
            } finally {
                txn.close();
            }
        }
    }

    @Override
    public void onManagementNodeJoined(List<? extends ManagementServerHost> nodeList, long selfNodeId) {
    }

    @Override
    public void onManagementNodeLeft(List<? extends ManagementServerHost> nodeList, long selfNodeId) {
        for (ManagementServerHost vo : nodeList) {
            s_logger.info("Marking hosts as disconnected on Management server" + vo.getMsid());
            long lastPing = (System.currentTimeMillis() >> 10) - getTimeout();
            _hostDao.markHostsAsDisconnected(vo.getMsid(), lastPing);
            s_logger.info("Deleting entries from op_host_transfer table for Management server " + vo.getMsid());
            cleanupTransferMap(vo.getMsid());
        }
    }

    @Override
    public void onManagementNodeIsolated() {
    }

    @Override
    public void removeAgent(AgentAttache attache, Status nextState) {
        if (attache == null) {
            return;
        }

        super.removeAgent(attache, nextState);
    }

    @Override
    public boolean executeRebalanceRequest(long agentId, long currentOwnerId, long futureOwnerId, Event event) throws AgentUnavailableException,
    OperationTimedoutException {
        boolean result = false;
        if (event == Event.RequestAgentRebalance) {
            return setToWaitForRebalance(agentId, currentOwnerId, futureOwnerId);
        } else if (event == Event.StartAgentRebalance) {
            try {
                result = rebalanceHost(agentId, currentOwnerId, futureOwnerId);
            } catch (Exception e) {
                s_logger.warn("Unable to rebalance host id=" + agentId, e);
            }
        }
        return result;
    }

    @Override
    public void scheduleRebalanceAgents() {
        _timer.schedule(new AgentLoadBalancerTask(), 30000);
    }

    public class AgentLoadBalancerTask extends ManagedContextTimerTask {
        protected volatile boolean cancelled = false;

        public AgentLoadBalancerTask() {
            s_logger.debug("Agent load balancer task created");
        }

        @Override
        public synchronized boolean cancel() {
            if (!cancelled) {
                cancelled = true;
                s_logger.debug("Agent load balancer task cancelled");
                return super.cancel();
            }
            return true;
        }

        @Override
        protected synchronized void runInContext() {
            try {
                if (!cancelled) {
                    startRebalanceAgents();
                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("The agent load balancer task is now being cancelled");
                    }
                    cancelled = true;
                }
            } catch (Throwable e) {
                s_logger.error("Unexpected exception " + e.toString(), e);
            }
        }
    }

    public void startRebalanceAgents() {
        s_logger.debug("Management server " + _nodeId + " is asking other peers to rebalance their agents");
        List<ManagementServerHostVO> allMS = _mshostDao.listBy(ManagementServerHost.State.Up);
        QueryBuilder<HostVO> sc = QueryBuilder.create(HostVO.class);
        sc.and(sc.entity().getManagementServerId(), Op.NNULL);
        sc.and(sc.entity().getType(), Op.EQ, Host.Type.Routing);
        List<HostVO> allManagedAgents = sc.list();

        int avLoad = 0;

        if (!allManagedAgents.isEmpty() && !allMS.isEmpty()) {
            avLoad = allManagedAgents.size() / allMS.size();
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("There are no hosts to rebalance in the system. Current number of active management server nodes in the system is " + allMS.size() +
                        "; number of managed agents is " + allManagedAgents.size());
            }
            return;
        }

        if (avLoad == 0L) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("As calculated average load is less than 1, rounding it to 1");
            }
            avLoad = 1;
        }

        for (ManagementServerHostVO node : allMS) {
            if (node.getMsid() != _nodeId) {

                List<HostVO> hostsToRebalance = new ArrayList<HostVO>();
                for (AgentLoadBalancerPlanner lbPlanner : _lbPlanners) {
                    hostsToRebalance = lbPlanner.getHostsToRebalance(node.getMsid(), avLoad);
                    if (hostsToRebalance != null && !hostsToRebalance.isEmpty()) {
                        break;
                    } else {
                        s_logger.debug("Agent load balancer planner " + lbPlanner.getName() + " found no hosts to be rebalanced from management server " + node.getMsid());
                    }
                }

                if (hostsToRebalance != null && !hostsToRebalance.isEmpty()) {
                    s_logger.debug("Found " + hostsToRebalance.size() + " hosts to rebalance from management server " + node.getMsid());
                    for (HostVO host : hostsToRebalance) {
                        long hostId = host.getId();
                        s_logger.debug("Asking management server " + node.getMsid() + " to give away host id=" + hostId);
                        boolean result = true;

                        if (_hostTransferDao.findById(hostId) != null) {
                            s_logger.warn("Somebody else is already rebalancing host id: " + hostId);
                            continue;
                        }

                        HostTransferMapVO transfer = null;
                        try {
                            transfer = _hostTransferDao.startAgentTransfering(hostId, node.getMsid(), _nodeId);
                            Answer[] answer = sendRebalanceCommand(node.getMsid(), hostId, node.getMsid(), _nodeId, Event.RequestAgentRebalance);
                            if (answer == null) {
                                s_logger.warn("Failed to get host id=" + hostId + " from management server " + node.getMsid());
                                result = false;
                            }
                        } catch (Exception ex) {
                            s_logger.warn("Failed to get host id=" + hostId + " from management server " + node.getMsid(), ex);
                            result = false;
                        } finally {
                            if (transfer != null) {
                                HostTransferMapVO transferState = _hostTransferDao.findByIdAndFutureOwnerId(transfer.getId(), _nodeId);
                                if (!result && transferState != null && transferState.getState() == HostTransferState.TransferRequested) {
                                    if (s_logger.isDebugEnabled()) {
                                        s_logger.debug("Removing mapping from op_host_transfer as it failed to be set to transfer mode");
                                    }
                                    // just remove the mapping (if exists) as nothing was done on the peer management
// server yet
                                    _hostTransferDao.remove(transfer.getId());
                                }
                            }
                        }
                    }
                } else {
                    s_logger.debug("Found no hosts to rebalance from the management server " + node.getMsid());
                }
            }
        }
    }

    private Answer[] sendRebalanceCommand(long peer, long agentId, long currentOwnerId, long futureOwnerId, Event event) {
        TransferAgentCommand transfer = new TransferAgentCommand(agentId, currentOwnerId, futureOwnerId, event);
        Commands commands = new Commands(Command.OnError.Stop);
        commands.addCommand(transfer);

        Command[] cmds = commands.toCommands();

        try {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Forwarding " + cmds[0].toString() + " to " + peer);
            }
            String peerName = Long.toString(peer);
            String cmdStr = _gson.toJson(cmds);
            String ansStr = _clusterMgr.execute(peerName, agentId, cmdStr, true);
            Answer[] answers = _gson.fromJson(ansStr, Answer[].class);
            return answers;
        } catch (Exception e) {
            s_logger.warn("Caught exception while talking to " + currentOwnerId, e);
            return null;
        }
    }

    public String getPeerName(long agentHostId) {

        HostVO host = _hostDao.findById(agentHostId);
        if (host != null && host.getManagementServerId() != null) {
            if (_clusterMgr.getSelfPeerName().equals(Long.toString(host.getManagementServerId()))) {
                return null;
            }

            return Long.toString(host.getManagementServerId());
        }
        return null;
    }

    public Boolean propagateAgentEvent(long agentId, Event event) throws AgentUnavailableException {
        final String msPeer = getPeerName(agentId);
        if (msPeer == null) {
            return null;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Propagating agent change request event:" + event.toString() + " to agent:" + agentId);
        }
        Command[] cmds = new Command[1];
        cmds[0] = new ChangeAgentCommand(agentId, event);

        String ansStr = _clusterMgr.execute(msPeer, agentId, _gson.toJson(cmds), true);
        if (ansStr == null) {
            throw new AgentUnavailableException(agentId);
        }

        Answer[] answers = _gson.fromJson(ansStr, Answer[].class);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Result for agent change is " + answers[0].getResult());
        }

        return answers[0].getResult();
    }

    private Runnable getTransferScanTask() {
        return new ManagedContextRunnable() {
            @Override
            protected void runInContext() {
                try {
                    if (s_logger.isTraceEnabled()) {
                        s_logger.trace("Clustered agent transfer scan check, management server id:" + _nodeId);
                    }
                    synchronized (_agentToTransferIds) {
                        if (_agentToTransferIds.size() > 0) {
                            s_logger.debug("Found " + _agentToTransferIds.size() + " agents to transfer");
                            // for (Long hostId : _agentToTransferIds) {
                            for (Iterator<Long> iterator = _agentToTransferIds.iterator(); iterator.hasNext();) {
                                Long hostId = iterator.next();
                                AgentAttache attache = findAttache(hostId);

                                // if the thread:
                                // 1) timed out waiting for the host to reconnect
                                // 2) recipient management server is not active any more
                                // 3) if the management server doesn't own the host any more
                                // remove the host from re-balance list and delete from op_host_transfer DB
                                // no need to do anything with the real attache as we haven't modified it yet
                                Date cutTime = DateUtil.currentGMTTime();
                                HostTransferMapVO transferMap =
                                        _hostTransferDao.findActiveHostTransferMapByHostId(hostId, new Date(cutTime.getTime() - rebalanceTimeOut));

                                if (transferMap == null) {
                                    s_logger.debug("Timed out waiting for the host id=" + hostId + " to be ready to transfer, skipping rebalance for the host");
                                    iterator.remove();
                                    _hostTransferDao.completeAgentTransfer(hostId);
                                    continue;
                                }

                                if (transferMap.getInitialOwner() != _nodeId || attache == null || attache.forForward()) {
                                    s_logger.debug("Management server " + _nodeId + " doesn't own host id=" + hostId + " any more, skipping rebalance for the host");
                                    iterator.remove();
                                    _hostTransferDao.completeAgentTransfer(hostId);
                                    continue;
                                }

                                ManagementServerHostVO ms = _mshostDao.findByMsid(transferMap.getFutureOwner());
                                if (ms != null && ms.getState() != ManagementServerHost.State.Up) {
                                    s_logger.debug("Can't transfer host " + hostId + " as it's future owner is not in UP state: " + ms +
                                            ", skipping rebalance for the host");
                                    iterator.remove();
                                    _hostTransferDao.completeAgentTransfer(hostId);
                                    continue;
                                }

                                if (attache.getQueueSize() == 0 && attache.getNonRecurringListenersSize() == 0) {
                                    iterator.remove();
                                    try {
                                        _executor.execute(new RebalanceTask(hostId, transferMap.getInitialOwner(), transferMap.getFutureOwner()));
                                    } catch (RejectedExecutionException ex) {
                                        s_logger.warn("Failed to submit rebalance task for host id=" + hostId + "; postponing the execution");
                                        continue;
                                    }

                                } else {
                                    s_logger.debug("Agent " + hostId + " can't be transfered yet as its request queue size is " + attache.getQueueSize() +
                                            " and listener queue size is " + attache.getNonRecurringListenersSize());
                                }
                            }
                        } else {
                            if (s_logger.isTraceEnabled()) {
                                s_logger.trace("Found no agents to be transfered by the management server " + _nodeId);
                            }
                        }
                    }

                } catch (Throwable e) {
                    s_logger.error("Problem with the clustered agent transfer scan check!", e);
                }
            }
        };
    }

    private boolean setToWaitForRebalance(final long hostId, long currentOwnerId, long futureOwnerId) {
        s_logger.debug("Adding agent " + hostId + " to the list of agents to transfer");
        synchronized (_agentToTransferIds) {
            return _agentToTransferIds.add(hostId);
        }
    }

    protected boolean rebalanceHost(final long hostId, long currentOwnerId, long futureOwnerId) throws AgentUnavailableException {

        boolean result = true;
        if (currentOwnerId == _nodeId) {
            if (!startRebalance(hostId)) {
                s_logger.debug("Failed to start agent rebalancing");
                finishRebalance(hostId, futureOwnerId, Event.RebalanceFailed);
                return false;
            }
            try {
                Answer[] answer = sendRebalanceCommand(futureOwnerId, hostId, currentOwnerId, futureOwnerId, Event.StartAgentRebalance);
                if (answer == null || !answer[0].getResult()) {
                    result = false;
                }

            } catch (Exception ex) {
                s_logger.warn("Host " + hostId + " failed to connect to the management server " + futureOwnerId + " as a part of rebalance process", ex);
                result = false;
            }

            if (result) {
                s_logger.debug("Successfully transfered host id=" + hostId + " to management server " + futureOwnerId);
                finishRebalance(hostId, futureOwnerId, Event.RebalanceCompleted);
            } else {
                s_logger.warn("Failed to transfer host id=" + hostId + " to management server " + futureOwnerId);
                finishRebalance(hostId, futureOwnerId, Event.RebalanceFailed);
            }

        } else if (futureOwnerId == _nodeId) {
            HostVO host = _hostDao.findById(hostId);
            try {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Disconnecting host " + host.getId() + "(" + host.getName() + " as a part of rebalance process without notification");
                }

                AgentAttache attache = findAttache(hostId);
                if (attache != null) {
                    result = handleDisconnect(attache, Event.AgentDisconnected, false, false, true);
                }

                if (result) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Loading directly connected host " + host.getId() + "(" + host.getName() + ") to the management server " + _nodeId +
                                " as a part of rebalance process");
                    }
                    result = loadDirectlyConnectedHost(host, true);
                } else {
                    s_logger.warn("Failed to disconnect " + host.getId() + "(" + host.getName() + " as a part of rebalance process without notification");
                }

            } catch (Exception ex) {
                s_logger.warn("Failed to load directly connected host " + host.getId() + "(" + host.getName() + ") to the management server " + _nodeId +
                        " as a part of rebalance process due to:", ex);
                result = false;
            }

            if (result) {
                s_logger.debug("Successfully loaded directly connected host " + host.getId() + "(" + host.getName() + ") to the management server " + _nodeId +
                        " as a part of rebalance process");
            } else {
                s_logger.warn("Failed to load directly connected host " + host.getId() + "(" + host.getName() + ") to the management server " + _nodeId +
                        " as a part of rebalance process");
            }
        }

        return result;
    }

    protected void finishRebalance(final long hostId, long futureOwnerId, Event event) {

        boolean success = (event == Event.RebalanceCompleted) ? true : false;
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Finishing rebalancing for the agent " + hostId + " with event " + event);
        }

        AgentAttache attache = findAttache(hostId);
        if (attache == null || !(attache instanceof ClusteredAgentAttache)) {
            s_logger.debug("Unable to find forward attache for the host id=" + hostId + ", assuming that the agent disconnected already");
            _hostTransferDao.completeAgentTransfer(hostId);
            return;
        }

        ClusteredAgentAttache forwardAttache = (ClusteredAgentAttache)attache;

        if (success) {

            // 1) Set transfer mode to false - so the agent can start processing requests normally
            forwardAttache.setTransferMode(false);

            // 2) Get all transfer requests and route them to peer
            Request requestToTransfer = forwardAttache.getRequestToTransfer();
            while (requestToTransfer != null) {
                s_logger.debug("Forwarding request " + requestToTransfer.getSequence() + " held in transfer attache " + hostId + " from the management server " +
                        _nodeId + " to " + futureOwnerId);
                boolean routeResult = routeToPeer(Long.toString(futureOwnerId), requestToTransfer.getBytes());
                if (!routeResult) {
                    logD(requestToTransfer.getBytes(), "Failed to route request to peer");
                }

                requestToTransfer = forwardAttache.getRequestToTransfer();
            }

            s_logger.debug("Management server " + _nodeId + " completed agent " + hostId + " rebalance to " + futureOwnerId);

        } else {
            failRebalance(hostId);
        }

        s_logger.debug("Management server " + _nodeId + " completed agent " + hostId + " rebalance");
        _hostTransferDao.completeAgentTransfer(hostId);
    }

    protected void failRebalance(final long hostId) {
        try {
            s_logger.debug("Management server " + _nodeId + " failed to rebalance agent " + hostId);
            _hostTransferDao.completeAgentTransfer(hostId);
            handleDisconnectWithoutInvestigation(findAttache(hostId), Event.RebalanceFailed, true, true);
        } catch (Exception ex) {
            s_logger.warn("Failed to reconnect host id=" + hostId + " as a part of failed rebalance task cleanup");
        }
    }

    protected boolean startRebalance(final long hostId) {
        HostVO host = _hostDao.findById(hostId);

        if (host == null || host.getRemoved() != null) {
            s_logger.warn("Unable to find host record, fail start rebalancing process");
            return false;
        }

        synchronized (_agents) {
            ClusteredDirectAgentAttache attache = (ClusteredDirectAgentAttache)_agents.get(hostId);
            if (attache != null && attache.getQueueSize() == 0 && attache.getNonRecurringListenersSize() == 0) {
                handleDisconnectWithoutInvestigation(attache, Event.StartAgentRebalance, true, true);
                ClusteredAgentAttache forwardAttache = (ClusteredAgentAttache)createAttache(hostId);
                if (forwardAttache == null) {
                    s_logger.warn("Unable to create a forward attache for the host " + hostId + " as a part of rebalance process");
                    return false;
                }
                s_logger.debug("Putting agent id=" + hostId + " to transfer mode");
                forwardAttache.setTransferMode(true);
                _agents.put(hostId, forwardAttache);
            } else {
                if (attache == null) {
                    s_logger.warn("Attache for the agent " + hostId + " no longer exists on management server " + _nodeId + ", can't start host rebalancing");
                } else {
                    s_logger.warn("Attache for the agent " + hostId + " has request queue size= " + attache.getQueueSize() + " and listener queue size " +
                            attache.getNonRecurringListenersSize() + ", can't start host rebalancing");
                }
                return false;
            }
        }
        _hostTransferDao.startAgentTransfer(hostId);
        return true;
    }

    protected void cleanupTransferMap(long msId) {
        List<HostTransferMapVO> hostsJoingingCluster = _hostTransferDao.listHostsJoiningCluster(msId);

        for (HostTransferMapVO hostJoingingCluster : hostsJoingingCluster) {
            _hostTransferDao.remove(hostJoingingCluster.getId());
        }

        List<HostTransferMapVO> hostsLeavingCluster = _hostTransferDao.listHostsLeavingCluster(msId);
        for (HostTransferMapVO hostLeavingCluster : hostsLeavingCluster) {
            _hostTransferDao.remove(hostLeavingCluster.getId());
        }
    }

    protected class RebalanceTask extends ManagedContextRunnable {
        Long hostId = null;
        Long currentOwnerId = null;
        Long futureOwnerId = null;

        public RebalanceTask(long hostId, long currentOwnerId, long futureOwnerId) {
            this.hostId = hostId;
            this.currentOwnerId = currentOwnerId;
            this.futureOwnerId = futureOwnerId;
        }

        @Override
        protected void runInContext() {
            try {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Rebalancing host id=" + hostId);
                }
                rebalanceHost(hostId, currentOwnerId, futureOwnerId);
            } catch (Exception e) {
                s_logger.warn("Unable to rebalance host id=" + hostId, e);
            }
        }
    }

    private String handleScheduleHostScanTaskCommand(ScheduleHostScanTaskCommand cmd) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Intercepting resource manager command: " + _gson.toJson(cmd));
        }

        try {
            scheduleHostScanTask();
        } catch (Exception e) {
            // Scheduling host scan task in peer MS is a best effort operation during host add, regular host scan
            // happens at fixed intervals anyways. So handling any exceptions that may be thrown
            s_logger.warn("Exception happened while trying to schedule host scan task on mgmt server " + _clusterMgr.getSelfPeerName() +
                    ", ignoring as regular host scan happens at fixed interval anyways", e);
            return null;
        }

        Answer[] answers = new Answer[1];
        answers[0] = new Answer(cmd, true, null);
        return _gson.toJson(answers);
    }

    public Answer[] sendToAgent(Long hostId, Command[] cmds, boolean stopOnError) throws AgentUnavailableException, OperationTimedoutException {
        Commands commands = new Commands(stopOnError ? Command.OnError.Stop : Command.OnError.Continue);
        for (Command cmd : cmds) {
            commands.addCommand(cmd);
        }
        return send(hostId, commands);
    }

    protected class ClusterDispatcher implements ClusterManager.Dispatcher {
        @Override
        public String getName() {
            return "ClusterDispatcher";
        }

        @Override
        public String dispatch(ClusterServicePdu pdu) {

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Dispatch ->" + pdu.getAgentId() + ", json: " + pdu.getJsonPackage());
            }

            Command[] cmds = null;
            try {
                cmds = _gson.fromJson(pdu.getJsonPackage(), Command[].class);
            } catch (Throwable e) {
                assert (false);
                s_logger.error("Excection in gson decoding : ", e);
            }

            if (cmds.length == 1 && cmds[0] instanceof ChangeAgentCommand) { // intercepted
                ChangeAgentCommand cmd = (ChangeAgentCommand)cmds[0];

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Intercepting command for agent change: agent " + cmd.getAgentId() + " event: " + cmd.getEvent());
                }
                boolean result = false;
                try {
                    result = executeAgentUserRequest(cmd.getAgentId(), cmd.getEvent());
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Result is " + result);
                    }

                } catch (AgentUnavailableException e) {
                    s_logger.warn("Agent is unavailable", e);
                    return null;
                }

                Answer[] answers = new Answer[1];
                answers[0] = new ChangeAgentAnswer(cmd, result);
                return _gson.toJson(answers);
            } else if (cmds.length == 1 && cmds[0] instanceof TransferAgentCommand) {
                TransferAgentCommand cmd = (TransferAgentCommand)cmds[0];

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Intercepting command for agent rebalancing: agent " + cmd.getAgentId() + " event: " + cmd.getEvent());
                }
                boolean result = false;
                try {
                    result = rebalanceAgent(cmd.getAgentId(), cmd.getEvent(), cmd.getCurrentOwner(), cmd.getFutureOwner());
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Result is " + result);
                    }

                } catch (AgentUnavailableException e) {
                    s_logger.warn("Agent is unavailable", e);
                    return null;
                } catch (OperationTimedoutException e) {
                    s_logger.warn("Operation timed out", e);
                    return null;
                }
                Answer[] answers = new Answer[1];
                answers[0] = new Answer(cmd, result, null);
                return _gson.toJson(answers);
            } else if (cmds.length == 1 && cmds[0] instanceof PropagateResourceEventCommand) {
                PropagateResourceEventCommand cmd = (PropagateResourceEventCommand)cmds[0];

                s_logger.debug("Intercepting command to propagate event " + cmd.getEvent().name() + " for host " + cmd.getHostId());

                boolean result = false;
                try {
                    result = _resourceMgr.executeUserRequest(cmd.getHostId(), cmd.getEvent());
                    s_logger.debug("Result is " + result);
                } catch (AgentUnavailableException ex) {
                    s_logger.warn("Agent is unavailable", ex);
                    return null;
                }

                Answer[] answers = new Answer[1];
                answers[0] = new Answer(cmd, result, null);
                return _gson.toJson(answers);
            } else if (cmds.length == 1 && cmds[0] instanceof ScheduleHostScanTaskCommand) {
                ScheduleHostScanTaskCommand cmd = (ScheduleHostScanTaskCommand)cmds[0];
                String response = handleScheduleHostScanTaskCommand(cmd);
                return response;
            }

            try {
                long startTick = System.currentTimeMillis();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Dispatch -> " + pdu.getAgentId() + ", json: " + pdu.getJsonPackage());
                }

                Answer[] answers = sendToAgent(pdu.getAgentId(), cmds, pdu.isStopOnError());
                if (answers != null) {
                    String jsonReturn = _gson.toJson(answers);

                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Completed dispatching -> " + pdu.getAgentId() + ", json: " + pdu.getJsonPackage() + " in " +
                                (System.currentTimeMillis() - startTick) + " ms, return result: " + jsonReturn);
                    }

                    return jsonReturn;
                } else {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Completed dispatching -> " + pdu.getAgentId() + ", json: " + pdu.getJsonPackage() + " in " +
                                (System.currentTimeMillis() - startTick) + " ms, return null result");
                    }
                }
            } catch (AgentUnavailableException e) {
                s_logger.warn("Agent is unavailable", e);
            } catch (OperationTimedoutException e) {
                s_logger.warn("Timed Out", e);
            }

            return null;
        }

    }

    public boolean executeAgentUserRequest(long agentId, Event event) throws AgentUnavailableException {
        return executeUserRequest(agentId, event);
    }

    public boolean rebalanceAgent(long agentId, Event event, long currentOwnerId, long futureOwnerId) throws AgentUnavailableException, OperationTimedoutException {
        return executeRebalanceRequest(agentId, currentOwnerId, futureOwnerId, event);
    }

    public boolean isAgentRebalanceEnabled() {
        return EnableLB.value();
    }

    private Runnable getAgentRebalanceScanTask() {
        return new ManagedContextRunnable() {
            @Override
            protected void runInContext() {
                try {
                    if (s_logger.isTraceEnabled()) {
                        s_logger.trace("Agent rebalance task check, management server id:" + _nodeId);
                    }
                    // initiate agent lb task will be scheduled and executed only once, and only when number of agents
// loaded exceeds _connectedAgentsThreshold
                    if (!_agentLbHappened) {
                        QueryBuilder<HostVO> sc = QueryBuilder.create(HostVO.class);
                        sc.and(sc.entity().getManagementServerId(), Op.NNULL);
                        sc.and(sc.entity().getType(), Op.EQ, Host.Type.Routing);
                        List<HostVO> allManagedRoutingAgents = sc.list();

                        sc = QueryBuilder.create(HostVO.class);
                        sc.and(sc.entity().getType(), Op.EQ, Host.Type.Routing);
                        List<HostVO> allAgents = sc.list();
                        double allHostsCount = allAgents.size();
                        double managedHostsCount = allManagedRoutingAgents.size();
                        if (allHostsCount > 0.0) {
                            double load = managedHostsCount / allHostsCount;
                            if (load >= ConnectedAgentThreshold.value()) {
                                s_logger.debug("Scheduling agent rebalancing task as the average agent load " + load + " is more than the threshold " +
                                        ConnectedAgentThreshold.value());
                                scheduleRebalanceAgents();
                                _agentLbHappened = true;
                            } else {
                                s_logger.debug("Not scheduling agent rebalancing task as the averages load " + load + " is less than the threshold " +
                                        ConnectedAgentThreshold.value());
                            }
                        }
                    }
                } catch (Throwable e) {
                    s_logger.error("Problem with the clustered agent transfer scan check!", e);
                }
            }
        };
    }

    @Override
    public void rescan() {
        // schedule a scan task immediately
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Scheduling a host scan task");
        }
        // schedule host scan task on current MS
        scheduleHostScanTask();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Notifying all peer MS to schedule host scan task");
        }
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        ConfigKey<?>[] keys = super.getConfigKeys();

        List<ConfigKey<?>> keysLst = new ArrayList<ConfigKey<?>>();
        keysLst.addAll(Arrays.asList(keys));
        keysLst.add(EnableLB);
        keysLst.add(ConnectedAgentThreshold);
        keysLst.add(LoadSize);
        keysLst.add(ScanInterval);
        return keysLst.toArray(new ConfigKey<?>[keysLst.size()]);
    }
}
