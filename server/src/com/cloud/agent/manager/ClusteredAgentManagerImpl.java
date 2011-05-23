/**
 * Copyright (c) 2008, 2009, VMOps Inc.
 *
 * This code is Copyrighted and must not be reused, modified, or redistributed without the explicit consent of VMOps.
 */
package com.cloud.agent.manager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CancelCommand;
import com.cloud.agent.api.ChangeAgentCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.TransferAgentCommand;
import com.cloud.agent.transport.Request;
import com.cloud.agent.transport.Request.Version;
import com.cloud.agent.transport.Response;
import com.cloud.api.commands.UpdateHostPasswordCmd;
import com.cloud.cluster.ClusterManager;
import com.cloud.cluster.ClusterManagerListener;
import com.cloud.cluster.ClusteredAgentRebalanceService;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.agentlb.AgentLoadBalancerPlanner;
import com.cloud.cluster.agentlb.HostTransferMapVO;
import com.cloud.cluster.agentlb.HostTransferMapVO.HostTransferState;
import com.cloud.cluster.agentlb.dao.HostTransferMapDao;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.Status.Event;
import com.cloud.resource.ServerResource;
import com.cloud.storage.resource.DummySecondaryStorageResource;
import com.cloud.user.User;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.nio.Link;
import com.cloud.utils.nio.Task;

@Local(value = { AgentManager.class, ClusteredAgentRebalanceService.class })
public class ClusteredAgentManagerImpl extends AgentManagerImpl implements ClusterManagerListener, ClusteredAgentRebalanceService {
    final static Logger s_logger = Logger.getLogger(ClusteredAgentManagerImpl.class);
    private static final ScheduledExecutorService s_transferExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Cluster-AgentTransferExecutor"));

    public final static long STARTUP_DELAY = 5000;
    public final static long SCAN_INTERVAL = 90000; // 90 seconds, it takes 60 sec for xenserver to fail login
    public final static int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 5; // 5 seconds
    public long _loadSize = 100;
    protected Set<Long> _agentToTransferIds = new HashSet<Long>();
    private final long rebalanceTimeOut = 300000; // 5 mins - after this time remove the agent from the transfer list 

    @Inject
    protected ClusterManager _clusterMgr = null;

    protected HashMap<String, SocketChannel> _peers;
    private final Timer _timer = new Timer("ClusteredAgentManager Timer");

    @Inject
    protected ManagementServerHostDao _mshostDao;
    @Inject
    protected HostTransferMapDao _hostTransferDao;
    
    @Inject(adapter = AgentLoadBalancerPlanner.class)
    protected Adapters<AgentLoadBalancerPlanner> _lbPlanners;

    protected ClusteredAgentManagerImpl() {
        super();
    }

    @Override
    public boolean configure(String name, Map<String, Object> xmlParams) throws ConfigurationException {
        _peers = new HashMap<String, SocketChannel>(7);
        _nodeId = _clusterMgr.getManagementNodeId();

        ConfigurationDao configDao = ComponentLocator.getCurrentLocator().getDao(ConfigurationDao.class);
        Map<String, String> params = configDao.getConfiguration(xmlParams);
        String value = params.get(Config.DirectAgentLoadSize.key());
        _loadSize = NumbersUtil.parseInt(value, 16);

        ClusteredAgentAttache.initialize(this);

        _clusterMgr.registerListener(this);

        return super.configure(name, xmlParams);
    }

    @Override
    public boolean start() {
        if (!super.start()) {
            return false;
        }
        _timer.schedule(new DirectAgentScanTimerTask(), STARTUP_DELAY, SCAN_INTERVAL);

        // schedule transfer scan executor - if agent LB is enabled
        if (_clusterMgr.isAgentRebalanceEnabled()) {
            s_transferExecutor.scheduleAtFixedRate(getTransferScanTask(), ClusteredAgentRebalanceService.DEFAULT_TRANSFER_CHECK_INTERVAL, ClusteredAgentRebalanceService.DEFAULT_TRANSFER_CHECK_INTERVAL,
                    TimeUnit.MILLISECONDS);
        }

        return true;
    }

    private void runDirectAgentScanTimerTask() {
        GlobalLock scanLock = GlobalLock.getInternLock("clustermgr.scan");
        try {
            if (scanLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
                try {
                    scanDirectAgentToLoad();
                } finally {
                    scanLock.unlock();
                }
            }
        } finally {
            scanLock.releaseRef();
        }
    }

    private void scanDirectAgentToLoad() {
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Begin scanning directly connected hosts");
        }

        // for agents that are self-managed, threshold to be considered as disconnected is 3 ping intervals
        long cutSeconds = (System.currentTimeMillis() >> 10) - (_pingInterval * 3);
        List<HostVO> hosts = _hostDao.findDirectAgentToLoad(cutSeconds, _loadSize);
        if (hosts != null && hosts.size() == _loadSize) {
            Long clusterId = hosts.get((int) (_loadSize - 1)).getClusterId();
            if (clusterId != null) {
                for (int i = (int) (_loadSize - 1); i > 0; i--) {
                    if (hosts.get(i).getClusterId() == clusterId) {
                        hosts.remove(i);
                    } else {
                        break;
                    }
                }
            }
        }
        if (hosts != null && hosts.size() > 0) {
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
                    loadDirectlyConnectedHost(host);
                } catch (Throwable e) {
                    s_logger.debug(" can not load directly connected host " + host.getId() + "(" + host.getName() + ") due to " + e.toString());
                }
            }
        }

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("End scanning directly connected hosts");
        }
    }

    private class DirectAgentScanTimerTask extends TimerTask {
        @Override
        public void run() {
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

    @Override
    public boolean cancelMaintenance(final long hostId) {
        try {
            Boolean result = _clusterMgr.propagateAgentEvent(hostId, Event.ResetRequested);

            if (result != null) {
                return result;
            }
        } catch (AgentUnavailableException e) {
            return false;
        }

        return super.cancelMaintenance(hostId);
    }

    protected AgentAttache createAttache(long id) {
        s_logger.debug("create forwarding ClusteredAgentAttache for " + id);
        final AgentAttache attache = new ClusteredAgentAttache(this, id);
        AgentAttache old = null;
        synchronized (_agents) {
            old = _agents.get(id);
            _agents.put(id, attache);
        }
        if (old != null) {
            old.disconnect(Status.Removed);
        }
        return attache;
    }

    @Override
    protected AgentAttache createAttache(long id, HostVO server, Link link) {
        s_logger.debug("create ClusteredAgentAttache for " + id);
        final AgentAttache attache = new ClusteredAgentAttache(this, id, link, server.getStatus() == Status.Maintenance || server.getStatus() == Status.ErrorInMaintenance
                || server.getStatus() == Status.PrepareForMaintenance);
        link.attach(attache);
        AgentAttache old = null;
        synchronized (_agents) {
            old = _agents.get(id);
            _agents.put(id, attache);
        }
        if (old != null) {
            old.disconnect(Status.Removed);
        }
        return attache;
    }

    @Override
    protected AgentAttache createAttache(long id, HostVO server, ServerResource resource) {
        if (resource instanceof DummySecondaryStorageResource) {
            return new DummyAttache(this, id, false);
        }
        s_logger.debug("create ClusteredDirectAgentAttache for " + id);
        final DirectAgentAttache attache = new ClusteredDirectAgentAttache(this, id, _nodeId, resource, server.getStatus() == Status.Maintenance || server.getStatus() == Status.ErrorInMaintenance
                || server.getStatus() == Status.PrepareForMaintenance, this);
        AgentAttache old = null;
        synchronized (_agents) {
            old = _agents.get(id);
            _agents.put(id, attache);
        }
        if (old != null) {
            old.disconnect(Status.Removed);
        }
        return attache;
    }

    @Override
    protected boolean handleDisconnect(AgentAttache attache, Status.Event event, boolean investigate) {
        return handleDisconnect(attache, event, investigate, true);
    }

    protected boolean handleDisconnect(AgentAttache agent, Status.Event event, boolean investigate, boolean broadcast) {
        if (agent == null) {
            return true;
        }

        if (super.handleDisconnect(agent, event, investigate)) {
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
                handleDisconnect(attache, Event.AgentDisconnected, false, false);
            }

            return true;
        } else {
            return super.executeUserRequest(hostId, event);
        }
    }

    @Override
    public boolean maintain(final long hostId) throws AgentUnavailableException {
        Boolean result = _clusterMgr.propagateAgentEvent(hostId, Event.MaintenanceRequested);
        if (result != null) {
            return result;
        }

        return super.maintain(hostId);
    }

    @Override
    public boolean reconnect(final long hostId) throws AgentUnavailableException {
        Boolean result = _clusterMgr.propagateAgentEvent(hostId, Event.ShutdownRequested);
        if (result != null) {
            return result;
        }

        return super.reconnect(hostId);
    }

    @Override
    @DB
    public boolean deleteHost(long hostId, boolean isForced, User caller) {
        try {
            Boolean result = _clusterMgr.propagateAgentEvent(hostId, Event.Remove);
            if (result != null) {
                return result;
            }
        } catch (AgentUnavailableException e) {
            return false;
        }

        return super.deleteHost(hostId, isForced, caller);
    }

    @Override
    public boolean updateHostPassword(UpdateHostPasswordCmd upasscmd) {
        if (upasscmd.getClusterId() == null) {
            // update agent attache password
            try {
                Boolean result = _clusterMgr.propagateAgentEvent(upasscmd.getHostId(), Event.UpdatePassword);
                if (result != null) {
                    return result;
                }
            } catch (AgentUnavailableException e) {
            }
        } else {
            // get agents for the cluster
            List<HostVO> hosts = _hostDao.listByCluster(upasscmd.getClusterId());
            for (HostVO h : hosts) {
                try {
                    Boolean result = _clusterMgr.propagateAgentEvent(h.getId(), Event.UpdatePassword);
                    if (result != null) {
                        return result;
                    }
                } catch (AgentUnavailableException e) {
                }
            }
        }
        return super.updateHostPassword(upasscmd);
    }

    public void notifyNodesInCluster(AgentAttache attache) {
        s_logger.debug("Notifying other nodes of to disconnect");
        Command[] cmds = new Command[] { new ChangeAgentCommand(attache.getId(), Event.AgentDisconnected) };
        _clusterMgr.broadcast(attache.getId(), cmds);
    }

    protected static void logT(byte[] bytes, final String msg) {
        s_logger.trace("Seq " + Request.getAgentId(bytes) + "-" + Request.getSequence(bytes) + ": MgmtId " + Request.getManagementServerId(bytes) + ": "
                + (Request.isRequest(bytes) ? "Req: " : "Resp: ") + msg);
    }

    protected static void logD(byte[] bytes, final String msg) {
        s_logger.debug("Seq " + Request.getAgentId(bytes) + "-" + Request.getSequence(bytes) + ": MgmtId " + Request.getManagementServerId(bytes) + ": "
                + (Request.isRequest(bytes) ? "Req: " : "Resp: ") + msg);
    }

    protected static void logI(byte[] bytes, final String msg) {
        s_logger.info("Seq " + Request.getAgentId(bytes) + "-" + Request.getSequence(bytes) + ": MgmtId " + Request.getManagementServerId(bytes) + ": "
                + (Request.isRequest(bytes) ? "Req: " : "Resp: ") + msg);
    }

    public boolean routeToPeer(String peer, byte[] bytes) {
        int i = 0;
        SocketChannel ch = null;
        while (i++ < 5) {
            ch = connectToPeer(peer, ch);
            if (ch == null) {
                try {
                    logD(bytes, "Unable to route to peer: " + Request.parse(bytes).toString());
                } catch (Exception e) {
                }
                return false;
            }
            try {
                if (s_logger.isDebugEnabled()) {
                    logD(bytes, "Routing to peer");
                }
                Link.write(ch, new ByteBuffer[] { ByteBuffer.wrap(bytes) });
                return true;
            } catch (IOException e) {
                try {
                    logI(bytes, "Unable to route to peer: " + Request.parse(bytes).toString() + " due to " + e.getMessage());
                } catch (Exception ex) {
                }
            }
        }
        return false;
    }

    public String findPeer(long hostId) {
        return _clusterMgr.getPeerName(hostId);
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
        }
    }

    public SocketChannel connectToPeer(String peerName, SocketChannel prevCh) {
        synchronized (_peers) {
            SocketChannel ch = _peers.get(peerName);
            if (prevCh != null) {
                try {
                    prevCh.close();
                } catch (Exception e) {
                }
            }
            if (ch == null || ch == prevCh) {
                ManagementServerHostVO ms = _clusterMgr.getPeer(peerName);
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
                try {
                    ch = SocketChannel.open(new InetSocketAddress(addr, _port));
                    ch.configureBlocking(true); // make sure we are working at blocking mode
                    ch.socket().setKeepAlive(true);
                    ch.socket().setSoTimeout(60 * 1000);
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Connection to peer opened: " + peerName + ", ip: " + ip);
                    }
                    _peers.put(peerName, ch);
                } catch (IOException e) {
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
        String peerName = _clusterMgr.getPeerName(hostId);
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
        if (agent == null) {
            if (host.getStatus() == Status.Up && (host.getManagementServerId() != null && host.getManagementServerId() != _nodeId)) {
                agent = createAttache(hostId);
            }
        }
        if (agent == null) {
            throw new AgentUnavailableException("Host is not in the right state", hostId);
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
                }
            }
        }
        _timer.cancel();
        s_transferExecutor.shutdownNow();
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
            Transaction txn = Transaction.open(Transaction.CLOUD_DB);
            try {
                if (task.getType() != Task.Type.DATA) {
                    super.doTask(task);
                    return;
                }

                final byte[] data = task.getData();
                Version ver = Request.getVersion(data);
                if (ver.ordinal() < Version.v3.ordinal()) {
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
                        CancelCommand cancel = (CancelCommand) cmds[0];
                        if (s_logger.isDebugEnabled()) {
                            logD(data, "Cancel request received");
                        }
                        agent.cancel(cancel.getSequence());
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
                                Routable cluster = (Routable) agent;
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
                            AgentAttache attache = (AgentAttache) link.attachment();
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
    public void onManagementNodeJoined(List<ManagementServerHostVO> nodeList, long selfNodeId) {
    }

    @Override
    public void onManagementNodeLeft(List<ManagementServerHostVO> nodeList, long selfNodeId) {
        for (ManagementServerHostVO vo : nodeList) {
            s_logger.info("Marking hosts as disconnected on Management server" + vo.getMsid());
            _hostDao.markHostsAsDisconnected(vo.getMsid());
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
    public boolean executeRebalanceRequest(long agentId, Event event) throws AgentUnavailableException, OperationTimedoutException {
        if (event == Event.RequestAgentRebalance) {
            return setToWaitForRebalance(agentId);
        } else if (event == Event.StartAgentRebalance) {
            return rebalanceHost(agentId);
        } 

        return true;
    }

    @Override
    public void startRebalanceAgents() {
        Date cutTime = DateUtil.currentGMTTime();
        List<ManagementServerHostVO> activeNodes = _mshostDao.getActiveList(new Date(cutTime.getTime() - ClusterManager.DEFAULT_HEARTBEAT_THRESHOLD));
        List<HostVO> allManagedAgents = _hostDao.listManagedAgents();

        long avLoad = 0L;

        if (!allManagedAgents.isEmpty() && !activeNodes.isEmpty()) {
            avLoad = allManagedAgents.size() / activeNodes.size();
        } else {
            return;
        }

        for (ManagementServerHostVO node : activeNodes) {
            if (node.getMsid() != _nodeId) {
                
                List<HostVO> hostsToRebalance = new ArrayList<HostVO>();
                for (AgentLoadBalancerPlanner lbPlanner : _lbPlanners) {
                    hostsToRebalance = lbPlanner.getHostsToRebalance(node.getMsid(), avLoad);
                    if (!hostsToRebalance.isEmpty()) {
                        break;
                    }
                }

                if (!hostsToRebalance.isEmpty()) {
                    //TODO - execute rebalance for every host; right now we are doing it for (0) host just for testing
                    for (HostVO host : hostsToRebalance) {
                        long hostId = host.getId();
                        s_logger.debug("Asking management server " + node.getMsid() + " to give away host id=" + hostId);
                        boolean result = true;
                        HostTransferMapVO transfer = _hostTransferDao.startAgentTransfering(hostId, _nodeId, node.getMsid());
                        try {
                            Answer[] answer = sendRebalanceCommand(hostId, _nodeId, Event.RequestAgentRebalance);
                            if (answer == null) {
                                s_logger.warn("Failed to get host id=" + hostId + " from management server " + node.getMsid());
                                result = false;
                            }
                        } catch (Exception ex) {
                            s_logger.warn("Failed to get host id=" + hostId + " from management server " + node.getMsid(), ex);
                            result = false;
                        } finally {
                            HostTransferMapVO updatedTransfer = _hostTransferDao.findById(transfer.getId());
                            if (!result && updatedTransfer.getState() == HostTransferState.TransferRequested) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Removing mapping from op_host_transfer as it failed to be set to transfer mode");
                                }
                                //just remove the mapping as nothing was done on the peer management server yet
                                _hostTransferDao.remove(transfer.getId());
                            }
                        }
                    }
                }
            }
        }
    }

    private Answer[] sendRebalanceCommand(long agentId, long peer, Event event) {
        TransferAgentCommand transfer = new TransferAgentCommand(agentId, peer, event);
        Commands commands = new Commands(OnError.Stop);
        commands.addCommand(transfer);

        Command[] cmds = commands.toCommands();

        String peerName = Long.toString(peer);

        try {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Forwarding " + cmds[0].toString() + " to " + peer);
            }
            Answer[] answers = _clusterMgr.execute(peerName, agentId, cmds, true);
            return answers;
        } catch (Exception e) {
            s_logger.warn("Caught exception while talking to " + peer, e);
            return null;
        }
    }

    private Runnable getTransferScanTask() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    // TODO - change to trace level later on
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Clustered agent transfer scan check, management server id:" + _nodeId);
                    }

                    if (_agentToTransferIds.size() > 0) {
                        s_logger.debug("Found " + _agentToTransferIds.size() + " agents to transfer");
                        for (Long hostId : _agentToTransferIds) {
                            AgentAttache attache = findAttache(hostId);
                            if (attache.getQueueSize() == 0 && attache.getListenersSize() == 0) {
                                boolean result = true;
                                _agentToTransferIds.remove(hostId);
                                try {
                                    result = rebalanceHost(hostId);
                                } finally {
                                    if (result) {
                                        finishRebalance(hostId, Event.RebalanceCompleted);
                                    } else {
                                        finishRebalance(hostId, Event.RebalanceFailed);
                                    }
                                }
                            } else {
                                // if we timed out waiting for the host to reconnect, remove host from rebalance list and mark it as failed to rebalance
                                // no need to do anything with the real attache
                                Date cutTime = DateUtil.currentGMTTime();
                                if (!(_hostTransferDao.isActive(hostId, new Date(cutTime.getTime() - rebalanceTimeOut)))) {
                                    s_logger.debug("Timed out waiting for the host id=" + hostId + " to be ready to transfer, failing rebalance for this host");
                                    _agentToTransferIds.remove(hostId);
                                    HostTransferMapVO transferMap = _hostTransferDao.findById(hostId);
                                    transferMap.setState(HostTransferState.TransferFailed);
                                    _hostTransferDao.update(hostId, transferMap);
                                }  
                            }
                        }
                    } else {
                        // TODO - change to trace level later on
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Found no agents to be transfered by the management server " + _nodeId);
                        }
                    }

                } catch (Throwable e) {
                    s_logger.error("Problem with the clustered agent transfer scan check!", e);
                }
            }
        };
    }
    
    
    private boolean setToWaitForRebalance(final long hostId) {
        s_logger.debug("Adding agent " + hostId + " to the list of agents to transfer");
        synchronized (_agentToTransferIds) {
            return  _agentToTransferIds.add(hostId);
        }
    }
    
    
    private boolean rebalanceHost(final long hostId) {
        HostTransferMapVO map = _hostTransferDao.findById(hostId);
        HostVO host = _hostDao.findById(hostId);
        
        boolean result = true;
        if (map.getInitialOwner() == _nodeId) {
            ClusteredDirectAgentAttache attache = (ClusteredDirectAgentAttache)findAttache(hostId);
            
            if (attache != null && !attache.getTransferMode()) {
                attache.setTransferMode(true);
                s_logger.debug("Putting agent id=" + hostId + " to transfer mode");
                _agents.put(hostId, attache);
                if (host != null && host.getRemoved() == null) {
                    host.setManagementServerId(null);
                    s_logger.debug("Updating host id=" + hostId + " with the status " + Status.Rebalance);
                    _hostDao.updateStatus(host, Event.StartAgentRebalance, _nodeId);
                }
                
                try {
                    Answer[] answer = sendRebalanceCommand(hostId, map.getFutureOwner(), Event.StartAgentRebalance);
                    if (answer == null) {
                        s_logger.warn("Host " + hostId + " failed to connect to the  management server " + map.getFutureOwner() + " as a part of rebalance process");
                        result = false;
                    }
                } catch (Exception ex) {
                    s_logger.warn("Host " + hostId + " failed to connect to the  management server " + map.getFutureOwner() + " as a part of rebalance process", ex);
                    result = false;
                }
                if (result) {
                    s_logger.debug("Got host id=" + hostId + " from management server " + map.getFutureOwner());
                } 
                
            } 
        } else if (map.getFutureOwner() == _nodeId) {
            try {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Loading directly connected host " + host.getId() + "(" + host.getName() + ") as a part of rebalance process");
                }
                //TODO - 1) no need to do vmfullSync/storageSetup on the agent side 2) Make sure that if connection fails, host goes from Rebalance state to Alert
                loadDirectlyConnectedHost(host);
            } catch (Exception ex) {
                s_logger.warn("Unable to load directly connected host " + host.getId() + " as a part of rebalance due to exception: ", ex);
            }
        }
        
        return result;
    
    }
    
    private boolean finishRebalance(final long hostId, Event event) {
        HostTransferMapVO map = _hostTransferDao.findById(hostId);
        AgentAttache attache = findAttache(hostId);
        
        if (attache == null) {
            s_logger.debug("Unable to find attache for the host id=" + hostId + ", assuming that the agent disconnected already");
            HostTransferState state = (event == Event.RebalanceCompleted) ? HostTransferState.TransferCompleted : HostTransferState.TransferFailed;
            map.setState(state);
            _hostTransferDao.update(hostId, map); 
            return true;
        }
        
        if (map.getInitialOwner() != _nodeId) {
            s_logger.warn("Why finish rebalance called not by initial host owner???");
            return false;
        }   
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Finishing rebalancing for the host id=" + hostId);
        }
        
        if (event == Event.RebalanceFailed) {
            ((ClusteredDirectAgentAttache) attache).setTransferMode(false);
            s_logger.debug("Rebalance failed for the host id=" + hostId);
            map.setState(HostTransferState.TransferFailed);
            _hostTransferDao.update(hostId, map);
        } else if (event == Event.RebalanceCompleted) {

            //1) Get all the requests remove transfer attache
            LinkedList<Request> requests = ((ClusteredDirectAgentAttache) attache).getRequests();
            removeAgent(attache, Status.Rebalance);
            
            //2) Create forward attache
            createAttache(hostId);
            
            //3) forward all the requests to the management server which owns the host now
            if (!requests.isEmpty()) {
                for (Request request : requests) {
                    routeToPeer(Long.toString(map.getFutureOwner()), request.getBytes());
                }
            }
            
            map.setState(HostTransferState.TransferCompleted);
            _hostTransferDao.update(hostId, map); 
            
            return true;
            
        }
        return true;
    }
}
