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
package com.cloud.cluster;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.AgentManager.OnError;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ChangeAgentAnswer;
import com.cloud.agent.api.ChangeAgentCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PropagateResourceEventCommand;
import com.cloud.agent.api.TransferAgentCommand;
import com.cloud.agent.api.ScheduleHostScanTaskCommand;
import com.cloud.agent.manager.ClusteredAgentManagerImpl;
import com.cloud.agent.manager.Commands;
import com.cloud.cluster.agentlb.dao.HostTransferMapDao;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.cluster.dao.ManagementServerHostPeerDao;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status.Event;
import com.cloud.host.dao.HostDao;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.serializer.GsonHelper;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Profiler;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ComponentLifecycle;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.ConnectionConcierge;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.SearchCriteria2;
import com.cloud.utils.db.SearchCriteriaService;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.events.SubscriptionMgr;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExceptionUtil;
import com.cloud.utils.mgmt.JmxUtil;
import com.cloud.utils.net.NetUtils;
import com.google.gson.Gson;

@Local(value = { ClusterManager.class })
public class ClusterManagerImpl extends ManagerBase implements ClusterManager {
    private static final Logger s_logger = Logger.getLogger(ClusterManagerImpl.class);

    private static final int EXECUTOR_SHUTDOWN_TIMEOUT = 1000; // 1 second
    private static final int DEFAULT_OUTGOING_WORKERS = 5; 

    private final List<ClusterManagerListener> _listeners = new ArrayList<ClusterManagerListener>();
    private final Map<Long, ManagementServerHostVO> _activePeers = new HashMap<Long, ManagementServerHostVO>();
    private int _heartbeatInterval = ClusterManager.DEFAULT_HEARTBEAT_INTERVAL;
    private int _heartbeatThreshold = ClusterManager.DEFAULT_HEARTBEAT_THRESHOLD;

    private final Map<String, ClusterService> _clusterPeers;
    private final Gson _gson;

    @Inject
    private AgentManager _agentMgr;
    @Inject
    private ClusteredAgentRebalanceService _rebalanceService;
    @Inject
    private ResourceManager _resourceMgr;

    private final ScheduledExecutorService _heartbeatScheduler = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Cluster-Heartbeat"));
    private final ExecutorService _notificationExecutor = Executors.newFixedThreadPool(1, new NamedThreadFactory("Cluster-Notification"));
    private final List<ClusterManagerMessage> _notificationMsgs = new ArrayList<ClusterManagerMessage>();
    private ConnectionConcierge _heartbeatConnection = null;

    private final ExecutorService _executor;

    private ClusterServiceAdapter _currentServiceAdapter;

    @Inject
    private List<ClusterServiceAdapter> _serviceAdapters;

    @Inject private ManagementServerHostDao _mshostDao;
    @Inject private ManagementServerHostPeerDao _mshostPeerDao;
    @Inject private HostDao _hostDao;
    @Inject private HostTransferMapDao _hostTransferDao;
    @Inject private ConfigurationDao _configDao;

    //
    // pay attention to _mshostId and _msid
    // _mshostId is the primary key of management host table
    // _msid is the unique persistent identifier that peer name is based upon
    //
    private Long _mshostId = null;
    protected long _msId = ManagementServerNode.getManagementServerId();
    protected long _runId = System.currentTimeMillis();

    private boolean _peerScanInited = false;

    private String _clusterNodeIP = "127.0.0.1";
    private boolean _agentLBEnabled = false;
    private double _connectedAgentsThreshold = 0.7;
    private static boolean _agentLbHappened = false;
    
    private final List<ClusterServicePdu> _clusterPduOutgoingQueue = new ArrayList<ClusterServicePdu>();
    private final List<ClusterServicePdu> _clusterPduIncomingQueue = new ArrayList<ClusterServicePdu>();
    private final Map<Long, ClusterServiceRequestPdu> _outgoingPdusWaitingForAck = new HashMap<Long, ClusterServiceRequestPdu>();
    
    public ClusterManagerImpl() {
        _clusterPeers = new HashMap<String, ClusterService>();

        _gson = GsonHelper.getGson();

        // executor to perform remote-calls in another thread context, to avoid potential
        // recursive remote calls between nodes
        //
        _executor = Executors.newCachedThreadPool(new NamedThreadFactory("Cluster-Worker"));
        setRunLevel(ComponentLifecycle.RUN_LEVEL_FRAMEWORK);
    }
    
    private void registerRequestPdu(ClusterServiceRequestPdu pdu) {
        synchronized(_outgoingPdusWaitingForAck) {
            _outgoingPdusWaitingForAck.put(pdu.getSequenceId(), pdu);
        }
    }
    
    private ClusterServiceRequestPdu popRequestPdu(long ackSequenceId) {
        synchronized(_outgoingPdusWaitingForAck) {
            if(_outgoingPdusWaitingForAck.get(ackSequenceId) != null) {
                ClusterServiceRequestPdu pdu = _outgoingPdusWaitingForAck.get(ackSequenceId);
                _outgoingPdusWaitingForAck.remove(ackSequenceId);
                return pdu;
            }
        }
        
        return null;
    }
    
    private void cancelClusterRequestToPeer(String strPeer) {
        List<ClusterServiceRequestPdu> candidates = new ArrayList<ClusterServiceRequestPdu>();
        synchronized(_outgoingPdusWaitingForAck) {
            for(Map.Entry<Long, ClusterServiceRequestPdu> entry : _outgoingPdusWaitingForAck.entrySet()) {
                if(entry.getValue().getDestPeer().equalsIgnoreCase(strPeer))
                    candidates.add(entry.getValue());
            }

            for(ClusterServiceRequestPdu pdu : candidates) {
                _outgoingPdusWaitingForAck.remove(pdu.getSequenceId());
            }
        }
        
        for(ClusterServiceRequestPdu pdu : candidates) {
            s_logger.warn("Cancel cluster request PDU to peer: " + strPeer + ", pdu: " + _gson.toJson(pdu));
            synchronized(pdu) {
                pdu.notifyAll();
            }
        }
    }
    
    private void addOutgoingClusterPdu(ClusterServicePdu pdu) {
    	synchronized(_clusterPduOutgoingQueue) {
    		_clusterPduOutgoingQueue.add(pdu);
    		_clusterPduOutgoingQueue.notifyAll();
    	}
    }
    
    private ClusterServicePdu popOutgoingClusterPdu(long timeoutMs) {
    	synchronized(_clusterPduOutgoingQueue) {
    		try {
				_clusterPduOutgoingQueue.wait(timeoutMs);
			} catch (InterruptedException e) {
			}
			
			if(_clusterPduOutgoingQueue.size() > 0) {
				ClusterServicePdu pdu = _clusterPduOutgoingQueue.get(0);
				_clusterPduOutgoingQueue.remove(0);
				return pdu;
			}
    	}
    	return null;
    }

    private void addIncomingClusterPdu(ClusterServicePdu pdu) {
    	synchronized(_clusterPduIncomingQueue) {
    		_clusterPduIncomingQueue.add(pdu);
    		_clusterPduIncomingQueue.notifyAll();
    	}
    }
    
    private ClusterServicePdu popIncomingClusterPdu(long timeoutMs) {
    	synchronized(_clusterPduIncomingQueue) {
    		try {
    			_clusterPduIncomingQueue.wait(timeoutMs);
			} catch (InterruptedException e) {
			}
			
			if(_clusterPduIncomingQueue.size() > 0) {
				ClusterServicePdu pdu = _clusterPduIncomingQueue.get(0);
				_clusterPduIncomingQueue.remove(0);
				return pdu;
			}
    	}
    	return null;
    }
    
    private Runnable getClusterPduSendingTask() {
        return new Runnable() {
            @Override
            public void run() {
                onSendingClusterPdu();
            }
        };
    }
    
    private Runnable getClusterPduNotificationTask() {
        return new Runnable() {
            @Override
            public void run() {
                onNotifyingClusterPdu();
            }
        };
    }
    
    private void onSendingClusterPdu() {
        while(true) {
            try {
                ClusterServicePdu pdu = popOutgoingClusterPdu(1000);
                if(pdu == null)
                	continue;
                	
                ClusterService peerService =  null;
                for(int i = 0; i < 2; i++) {
                    try {
                        peerService = getPeerService(pdu.getDestPeer());
                    } catch (RemoteException e) {
                        s_logger.error("Unable to get cluster service on peer : " + pdu.getDestPeer());
                    }

                    if(peerService != null) {
                        try {
                            if(s_logger.isDebugEnabled()) {
                                s_logger.debug("Cluster PDU " + getSelfPeerName() + " -> " + pdu.getDestPeer() + ". agent: " + pdu.getAgentId() 
                                    + ", pdu seq: " + pdu.getSequenceId() + ", pdu ack seq: " + pdu.getAckSequenceId() + ", json: " + pdu.getJsonPackage());
                            }

                            long startTick = System.currentTimeMillis();
                            String strResult = peerService.execute(pdu);
                            if(s_logger.isDebugEnabled()) {
                                s_logger.debug("Cluster PDU " + getSelfPeerName() + " -> " + pdu.getDestPeer() + " completed. time: " +
                                    (System.currentTimeMillis() - startTick) + "ms. agent: " + pdu.getAgentId() 
                                     + ", pdu seq: " + pdu.getSequenceId() + ", pdu ack seq: " + pdu.getAckSequenceId() + ", json: " + pdu.getJsonPackage());
                            }
                            
                            if("true".equals(strResult))
                                break;
                            
                        } catch (RemoteException e) {
                            invalidatePeerService(pdu.getDestPeer());
                            if(s_logger.isInfoEnabled()) {
                                s_logger.info("Exception on remote execution, peer: " + pdu.getDestPeer() + ", iteration: "
                                        + i + ", exception message :" + e.getMessage());
                            }
                        }
                    }
                }
            } catch(Throwable e) {
                s_logger.error("Unexcpeted exception: ", e);
            }
        }
    }
    
    private void onNotifyingClusterPdu() {
        while(true) {
            try {
                final ClusterServicePdu pdu = popIncomingClusterPdu(1000);
                if(pdu == null)
                	continue;

                _executor.execute(new Runnable() {
                    @Override
                	public void run() {
		                if(pdu.getPduType() == ClusterServicePdu.PDU_TYPE_RESPONSE) {
		                    ClusterServiceRequestPdu requestPdu = popRequestPdu(pdu.getAckSequenceId());
		                    if(requestPdu != null) {
		                        requestPdu.setResponseResult(pdu.getJsonPackage());
		                        synchronized(requestPdu) {
		                            requestPdu.notifyAll();
		                        }
		                    } else {
		                        s_logger.warn("Original request has already been cancelled. pdu: " + _gson.toJson(pdu));
		                    }
		                } else {
		                    String result = dispatchClusterServicePdu(pdu);
		                    if(result == null)
		                        result = "";
		                    
		                    if(pdu.getPduType() == ClusterServicePdu.PDU_TYPE_REQUEST) {
			                    ClusterServicePdu responsePdu = new ClusterServicePdu();
			                    responsePdu.setPduType(ClusterServicePdu.PDU_TYPE_RESPONSE);
			                    responsePdu.setSourcePeer(pdu.getDestPeer());
			                    responsePdu.setDestPeer(pdu.getSourcePeer());
			                    responsePdu.setAckSequenceId(pdu.getSequenceId());
			                    responsePdu.setJsonPackage(result);
			                    
			                    addOutgoingClusterPdu(responsePdu);
		                    }
		                }
                	}
                });
            } catch(Throwable e) {
                s_logger.error("Unexcpeted exception: ", e);
            }
        }
    }

    private String handleScheduleHostScanTaskCommand(ScheduleHostScanTaskCommand cmd) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Intercepting resource manager command: " + _gson.toJson(cmd));
        }

        try {
            // schedule a scan task immediately
            if (ComponentContext.getTargetObject(_agentMgr) instanceof ClusteredAgentManagerImpl) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Received notification as part of addHost command to start a host scan task");
                }
                ClusteredAgentManagerImpl clusteredAgentMgr = (ClusteredAgentManagerImpl)ComponentContext.getTargetObject(_agentMgr);
                clusteredAgentMgr.scheduleHostScanTask();
            }
        } catch (Exception e) {
            // Scheduling host scan task in peer MS is a best effort operation during host add, regular host scan
            // happens at fixed intervals anyways. So handling any exceptions that may be thrown
            s_logger.warn("Exception happened while trying to schedule host scan task on mgmt server " + getSelfPeerName() + ", ignoring as regular host scan happens at fixed interval anyways", e);
            return null;
        }

        Answer[] answers = new Answer[1];
        answers[0] = new Answer(cmd, true, null);
        return _gson.toJson(answers);
    }

    private String dispatchClusterServicePdu(ClusterServicePdu pdu) {

        if(s_logger.isDebugEnabled()) {
            s_logger.debug("Dispatch ->" + pdu.getAgentId() + ", json: " + pdu.getJsonPackage());
        }

        Command [] cmds = null;
        try {
            cmds = _gson.fromJson(pdu.getJsonPackage(), Command[].class);
        } catch(Throwable e) {
            assert(false);
            s_logger.error("Excection in gson decoding : ", e);
        }
        
        if (cmds.length == 1 && cmds[0] instanceof ChangeAgentCommand) {  //intercepted
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
            TransferAgentCommand cmd = (TransferAgentCommand) cmds[0];

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
        } else if (cmds.length == 1 && cmds[0] instanceof PropagateResourceEventCommand ) {
        	PropagateResourceEventCommand cmd = (PropagateResourceEventCommand) cmds[0];
        	
        	s_logger.debug("Intercepting command to propagate event " + cmd.getEvent().name() + " for host " + cmd.getHostId());
        	
        	boolean result = false;
        	try {
        		result = executeResourceUserRequest(cmd.getHostId(), cmd.getEvent());
        		s_logger.debug("Result is " + result);
        	} catch (AgentUnavailableException ex) {
        		s_logger.warn("Agent is unavailable", ex);
        		return null;
        	}
        	
        	Answer[] answers = new Answer[1];
        	answers[0] = new Answer(cmd, result, null);
        	return _gson.toJson(answers);
        } else if (cmds.length == 1 && cmds[0] instanceof ScheduleHostScanTaskCommand) {
            ScheduleHostScanTaskCommand cmd = (ScheduleHostScanTaskCommand) cmds[0];
            String response = handleScheduleHostScanTaskCommand(cmd);
            return response;
        }

        try {
            long startTick = System.currentTimeMillis();
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("Dispatch -> " + pdu.getAgentId() + ", json: " + pdu.getJsonPackage());
            }

            Answer[] answers = sendToAgent(pdu.getAgentId(), cmds, pdu.isStopOnError());
            if(answers != null) {
                String jsonReturn =  _gson.toJson(answers);

                if(s_logger.isDebugEnabled()) {
                    s_logger.debug("Completed dispatching -> " + pdu.getAgentId() + ", json: " + pdu.getJsonPackage() +
                            " in " + (System.currentTimeMillis() - startTick) + " ms, return result: " + jsonReturn);
                }

                return jsonReturn;
            } else {
                if(s_logger.isDebugEnabled()) {
                    s_logger.debug("Completed dispatching -> " + pdu.getAgentId() + ", json: " + pdu.getJsonPackage() +
                            " in " + (System.currentTimeMillis() - startTick) + " ms, return null result");
                }
            }
        } catch(AgentUnavailableException e) {
            s_logger.warn("Agent is unavailable", e);
        } catch (OperationTimedoutException e) {
            s_logger.warn("Timed Out", e);
        }
        
        return null;
    }

    @Override
    public void OnReceiveClusterServicePdu(ClusterServicePdu pdu) {
    	addIncomingClusterPdu(pdu);
    }
    
    @Override
    public Answer[] sendToAgent(Long hostId, Command[] cmds, boolean stopOnError) throws AgentUnavailableException, OperationTimedoutException {
        Commands commands = new Commands(stopOnError ? OnError.Stop : OnError.Continue);
        for (Command cmd : cmds) {
            commands.addCommand(cmd);
        }
        return _agentMgr.send(hostId, commands);
    }

    @Override
    public boolean executeAgentUserRequest(long agentId, Event event) throws AgentUnavailableException {
        return _agentMgr.executeUserRequest(agentId, event);
    }

    @Override
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

        Answer[] answers = execute(msPeer, agentId, cmds, true);
        if (answers == null) {
            throw new AgentUnavailableException(agentId);
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Result for agent change is " + answers[0].getResult());
        }

        return answers[0].getResult();
    }

    /**
     * called by DatabaseUpgradeChecker to see if there are other peers running.
     * 
     * @param notVersion
     *            If version is passed in, the peers CANNOT be running at this version. If version is null, return true if any
     *            peer is running regardless of version.
     * @return true if there are peers running and false if not.
     */
    public static final boolean arePeersRunning(String notVersion) {
        return false; // TODO: Leaving this for Kelven to take care of.
    }

    @Override
    public void broadcast(long agentId, Command[] cmds) {
        Date cutTime = DateUtil.currentGMTTime();

        List<ManagementServerHostVO> peers = _mshostDao.getActiveList(new Date(cutTime.getTime() - _heartbeatThreshold));
        for (ManagementServerHostVO peer : peers) {
            String peerName = Long.toString(peer.getMsid());
            if (getSelfPeerName().equals(peerName)) {
                continue; // Skip myself.
            }
            try {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Forwarding " + cmds[0].toString() + " to " + peer.getMsid());
                }
                executeAsync(peerName, agentId, cmds, true);
            } catch (Exception e) {
                s_logger.warn("Caught exception while talkign to " + peer.getMsid());
            }
        }
    }

    @Override
    public void executeAsync(String strPeer, long agentId, Command [] cmds, boolean stopOnError) {
        ClusterServicePdu pdu = new ClusterServicePdu();
        pdu.setSourcePeer(getSelfPeerName());
        pdu.setDestPeer(strPeer);
        pdu.setAgentId(agentId);
        pdu.setJsonPackage(_gson.toJson(cmds, Command[].class));
        pdu.setStopOnError(true);
        addOutgoingClusterPdu(pdu);
    }

    @Override
    public Answer[] execute(String strPeer, long agentId, Command [] cmds, boolean stopOnError) {
        if(s_logger.isDebugEnabled()) {
            s_logger.debug(getSelfPeerName() + " -> " + strPeer + "." + agentId + " " +
                    _gson.toJson(cmds, Command[].class));
        }
        
        ClusterServiceRequestPdu pdu = new ClusterServiceRequestPdu();
        pdu.setSourcePeer(getSelfPeerName());
        pdu.setDestPeer(strPeer);
        pdu.setAgentId(agentId);
        pdu.setJsonPackage(_gson.toJson(cmds, Command[].class));
        pdu.setStopOnError(stopOnError);
        registerRequestPdu(pdu);
        addOutgoingClusterPdu(pdu);
        
        synchronized(pdu) {
            try {
                pdu.wait();
            } catch (InterruptedException e) {
            }
        }

        if(s_logger.isDebugEnabled()) {
            s_logger.debug(getSelfPeerName() + " -> " + strPeer + "." + agentId + " completed. result: " +
                pdu.getResponseResult());
        }
        
        if(pdu.getResponseResult() != null && pdu.getResponseResult().length() > 0) {
            try {
                return _gson.fromJson(pdu.getResponseResult(), Answer[].class);
            } catch(Throwable e) {
                s_logger.error("Exception on parsing gson package from remote call to " + strPeer);
            }
        }

        return null;
    }
    
    @Override
    public String getPeerName(long agentHostId) {

        HostVO host = _hostDao.findById(agentHostId);
        if(host != null && host.getManagementServerId() != null) {
            if(getSelfPeerName().equals(Long.toString(host.getManagementServerId()))) {
                return null;
            }

            return Long.toString(host.getManagementServerId());
        }
        return null;
    }

    @Override
    public ManagementServerHostVO getPeer(String mgmtServerId) {
        return _mshostDao.findByMsid(Long.valueOf(mgmtServerId));
    }

    @Override
    public String getSelfPeerName() {
        return Long.toString(_msId);
    }

    @Override
    public String getSelfNodeIP() {
        return _clusterNodeIP;
    }

    @Override
    public void registerListener(ClusterManagerListener listener) {
        // Note : we don't check duplicates
        synchronized (_listeners) {

    		s_logger.info("register cluster listener " + listener.getClass());
    		
        	_listeners.add(listener);
        }
    }

    @Override
    public void unregisterListener(ClusterManagerListener listener) {
        synchronized(_listeners) {
    		s_logger.info("unregister cluster listener " + listener.getClass());
        	
        	_listeners.remove(listener);
        }
    }

    public void notifyNodeJoined(List<ManagementServerHostVO> nodeList) {
        if(s_logger.isDebugEnabled()) {
            s_logger.debug("Notify management server node join to listeners.");

            for(ManagementServerHostVO mshost : nodeList) {
                s_logger.debug("Joining node, IP: " + mshost.getServiceIP() + ", msid: " + mshost.getMsid());
            }
        }

        synchronized(_listeners) {
            for(ClusterManagerListener listener : _listeners) {
                listener.onManagementNodeJoined(nodeList, _mshostId);
            }
        }

        SubscriptionMgr.getInstance().notifySubscribers(ClusterManager.ALERT_SUBJECT, this,
                new ClusterNodeJoinEventArgs(_mshostId, nodeList));
    }

    public void notifyNodeLeft(List<ManagementServerHostVO> nodeList) {
        if(s_logger.isDebugEnabled()) {
            s_logger.debug("Notify management server node left to listeners.");
        }
        
        for(ManagementServerHostVO mshost : nodeList) {
            if(s_logger.isDebugEnabled())
                s_logger.debug("Leaving node, IP: " + mshost.getServiceIP() + ", msid: " + mshost.getMsid());
            cancelClusterRequestToPeer(String.valueOf(mshost.getMsid()));
        }

        synchronized(_listeners) {
            for(ClusterManagerListener listener : _listeners) {
                listener.onManagementNodeLeft(nodeList, _mshostId);
            }
        }

        SubscriptionMgr.getInstance().notifySubscribers(ClusterManager.ALERT_SUBJECT, this,
                new ClusterNodeLeftEventArgs(_mshostId, nodeList));
    }

    public void notifyNodeIsolated() {
        if(s_logger.isDebugEnabled())
            s_logger.debug("Notify management server node isolation to listeners");

        synchronized(_listeners) {
            for(ClusterManagerListener listener : _listeners) {
                listener.onManagementNodeIsolated();
            }
        }
    }

    public ClusterService getPeerService(String strPeer) throws RemoteException {
        synchronized(_clusterPeers) {
            if(_clusterPeers.containsKey(strPeer)) {
                return _clusterPeers.get(strPeer);
            }
        }

        ClusterService service = _currentServiceAdapter.getPeerService(strPeer);

        if(service != null) {
            synchronized(_clusterPeers) {
                // re-check the peer map again to deal with the
                // race conditions
                if(!_clusterPeers.containsKey(strPeer)) {
                    _clusterPeers.put(strPeer, service);
                }
            }
        }

        return service;
    }

    public void invalidatePeerService(String strPeer) {
        synchronized(_clusterPeers) {
            if(_clusterPeers.containsKey(strPeer)) {
                _clusterPeers.remove(strPeer);
            }
        }
    }

    private Runnable getHeartbeatTask() {
        return new Runnable() {
            @Override
            public void run() {
                Transaction txn = Transaction.open("ClusterHeartBeat");
                try {
                    Profiler profiler = new Profiler();
                    Profiler profilerHeartbeatUpdate = new Profiler();
                    Profiler profilerPeerScan = new Profiler();
                    Profiler profilerAgentLB = new Profiler();
                    
                    try {
                        profiler.start();
                        
                        profilerHeartbeatUpdate.start();
                        txn.transitToUserManagedConnection(getHeartbeatConnection());
                        if(s_logger.isTraceEnabled()) {
                            s_logger.trace("Cluster manager heartbeat update, id:" + _mshostId);
                        }
    
                        _mshostDao.update(_mshostId, getCurrentRunId(), DateUtil.currentGMTTime());
                        profilerHeartbeatUpdate.stop();
    
                        profilerPeerScan.start();
                        if (s_logger.isTraceEnabled()) {
                            s_logger.trace("Cluster manager peer-scan, id:" + _mshostId);
                        }
    
                        if (!_peerScanInited) {
                            _peerScanInited = true;
                            initPeerScan();
                        }
                        
                        peerScan();
                        profilerPeerScan.stop();
                        
                        profilerAgentLB.start();
                        //initiate agent lb task will be scheduled and executed only once, and only when number of agents loaded exceeds _connectedAgentsThreshold
                        if (_agentLBEnabled && !_agentLbHappened) {
                            SearchCriteriaService<HostVO, HostVO> sc = SearchCriteria2.create(HostVO.class);
                            sc.addAnd(sc.getEntity().getManagementServerId(), Op.NNULL);
                            sc.addAnd(sc.getEntity().getType(), Op.EQ, Host.Type.Routing);
                            List<HostVO> allManagedRoutingAgents = sc.list();
                            
                            sc = SearchCriteria2.create(HostVO.class);
                            sc.addAnd(sc.getEntity().getType(), Op.EQ, Host.Type.Routing);
                            List<HostVO> allAgents = sc.list();
                            double allHostsCount = allAgents.size();
                            double managedHostsCount = allManagedRoutingAgents.size();
                            if (allHostsCount > 0.0) {
                                double load = managedHostsCount/allHostsCount;
                                if (load >= _connectedAgentsThreshold) {
                                    s_logger.debug("Scheduling agent rebalancing task as the average agent load " + load + " is more than the threshold " + _connectedAgentsThreshold);
                                    _rebalanceService.scheduleRebalanceAgents();
                                    _agentLbHappened = true;
                                } else {
                                    s_logger.trace("Not scheduling agent rebalancing task as the averages load " + load + " is less than the threshold " + _connectedAgentsThreshold);
                                }
                            } 
                        }
                        profilerAgentLB.stop();
                    } finally {
                        profiler.stop();
                        
                        if(profiler.getDuration() >= _heartbeatInterval) {
                            if(s_logger.isDebugEnabled())
                                s_logger.debug("Management server heartbeat takes too long to finish. profiler: " + profiler.toString() + 
                                    ", profilerHeartbeatUpdate: " + profilerHeartbeatUpdate.toString() +
                                    ", profilerPeerScan: " + profilerPeerScan.toString() +
                                    ", profilerAgentLB: " + profilerAgentLB.toString());
                        }
                    }
                    
                } catch(CloudRuntimeException e) {
                    s_logger.error("Runtime DB exception ", e.getCause());

                    if(e.getCause() instanceof ClusterInvalidSessionException) {
                        s_logger.error("Invalid cluster session found, fence it");
                        queueNotification(new ClusterManagerMessage(ClusterManagerMessage.MessageType.nodeIsolated));
                    }

                    if(isRootCauseConnectionRelated(e.getCause())) {
                        s_logger.error("DB communication problem detected, fence it");
                        queueNotification(new ClusterManagerMessage(ClusterManagerMessage.MessageType.nodeIsolated));
                    }

                    invalidHeartbeatConnection();
                } catch(ActiveFencingException e) {
                    queueNotification(new ClusterManagerMessage(ClusterManagerMessage.MessageType.nodeIsolated));
                } catch (Throwable e) {
                    s_logger.error("Unexpected exception in cluster heartbeat", e);
                    if(isRootCauseConnectionRelated(e.getCause())) {
                        s_logger.error("DB communication problem detected, fence it");
                        queueNotification(new ClusterManagerMessage(ClusterManagerMessage.MessageType.nodeIsolated));
                    }

                    invalidHeartbeatConnection();
                } finally {
                    txn.transitToAutoManagedConnection(Transaction.CLOUD_DB);                         
                    txn.close("ClusterHeartBeat");           	
                }
            }
        };
    }

    private boolean isRootCauseConnectionRelated(Throwable e) {
        while (e != null) {
            if (e instanceof SQLRecoverableException) {
                return true;
            }

            e = e.getCause();
        }

        return false;
    }

    private Connection getHeartbeatConnection() throws SQLException {
        if(_heartbeatConnection == null) {
            Connection conn = Transaction.getStandaloneConnectionWithException();
            _heartbeatConnection = new ConnectionConcierge("ClusterManagerHeartBeat", conn, false);
        }

        return _heartbeatConnection.conn();
    }

    private void invalidHeartbeatConnection() {
        if(_heartbeatConnection != null) {
            Connection conn = Transaction.getStandaloneConnection();
            if (conn != null) {
                _heartbeatConnection.reset(Transaction.getStandaloneConnection());
            }
        }
    }

    private Runnable getNotificationTask() {
        return new Runnable() {
            @Override
            public void run() {
                while(true) {
                    synchronized(_notificationMsgs) {
                        try {
                            _notificationMsgs.wait(1000);
                        } catch (InterruptedException e) {
                        }
                    }

                    ClusterManagerMessage msg = null;
                    while((msg = getNextNotificationMessage()) != null) {
                        try {
                            switch(msg.getMessageType()) {
                            case nodeAdded:
                                if(msg.getNodes() != null && msg.getNodes().size() > 0) {
                                    Profiler profiler = new Profiler();
                                    profiler.start();

                                    notifyNodeJoined(msg.getNodes());

                                    profiler.stop();
                                    if(profiler.getDuration() > 1000) {
                                        if(s_logger.isDebugEnabled()) {
                                            s_logger.debug("Notifying management server join event took " + profiler.getDuration() + " ms");
                                        }
                                    } else {
                                        s_logger.warn("Notifying management server join event took " + profiler.getDuration() + " ms");
                                    }
                                }
                                break;

                            case nodeRemoved:
                                if(msg.getNodes() != null && msg.getNodes().size() > 0) {
                                    Profiler profiler = new Profiler();
                                    profiler.start();

                                    notifyNodeLeft(msg.getNodes());

                                    profiler.stop();
                                    if(profiler.getDuration() > 1000) {
                                        if(s_logger.isDebugEnabled()) {
                                            s_logger.debug("Notifying management server leave event took " + profiler.getDuration() + " ms");
                                        }
                                    } else {
                                        s_logger.warn("Notifying management server leave event took " + profiler.getDuration() + " ms");
                                    }
                                }
                                break;

                            case nodeIsolated:
                                notifyNodeIsolated();
                                break;

                            default :
                                assert(false);
                                break;
                            }

                        } catch (Throwable e) {
                            s_logger.warn("Unexpected exception during cluster notification. ", e);
                        }
                    }

                    try { Thread.sleep(1000); } catch (InterruptedException e) {}
                }
            }
        };
    }

    private void queueNotification(ClusterManagerMessage msg) {
        synchronized(this._notificationMsgs) {
            this._notificationMsgs.add(msg);
            this._notificationMsgs.notifyAll();
        }
        
        switch(msg.getMessageType()) {
        case nodeAdded:
            {
                List<ManagementServerHostVO> l = msg.getNodes();
                if(l != null && l.size() > 0) {
                    for(ManagementServerHostVO mshost: l) {
                        _mshostPeerDao.updatePeerInfo(_mshostId, mshost.getId(), mshost.getRunid(), ManagementServerHost.State.Up);
                    }
                }
            }
            break;
            
        case nodeRemoved:
            {
                List<ManagementServerHostVO> l = msg.getNodes();
                if(l != null && l.size() > 0) {
                    for(ManagementServerHostVO mshost: l) {
                        _mshostPeerDao.updatePeerInfo(_mshostId, mshost.getId(), mshost.getRunid(), ManagementServerHost.State.Down);
                    }
                }
            }
            break;
            
        default :
            break;
        
        }
    }

    private ClusterManagerMessage getNextNotificationMessage() {
        synchronized(this._notificationMsgs) {
            if(this._notificationMsgs.size() > 0) {
                return this._notificationMsgs.remove(0);
            }
        }

        return null;
    }

    private void initPeerScan() {
        // upon startup, for all inactive management server nodes that we see at startup time, we will send notification also to help upper layer perform
        // missed cleanup
        Date cutTime = DateUtil.currentGMTTime();
        List<ManagementServerHostVO> inactiveList = _mshostDao.getInactiveList(new Date(cutTime.getTime() - _heartbeatThreshold));
       
        // We don't have foreign key constraints to enforce the mgmt_server_id integrity in host table, when user manually 
        // remove records from mshost table, this will leave orphan mgmt_serve_id reference in host table.
        List<Long> orphanList = _mshostDao.listOrphanMsids();
        if(orphanList.size() > 0) {
	        for(Long orphanMsid : orphanList) {
	        	// construct fake ManagementServerHostVO based on orphan MSID
	        	s_logger.info("Add orphan management server msid found in host table to initial clustering notification, orphan msid: " + orphanMsid);
	        	inactiveList.add(new ManagementServerHostVO(orphanMsid, 0, "orphan", 0, new Date()));
	        }
        } else {
        	s_logger.info("We are good, no orphan management server msid in host table is found");
        }
        
        if(inactiveList.size() > 0) {
        	if(s_logger.isInfoEnabled()) {
        		s_logger.info("Found " + inactiveList.size() + " inactive management server node based on timestamp");
        		for(ManagementServerHostVO host : inactiveList)
        			s_logger.info("management server node msid: " + host.getMsid() + ", name: " + host.getName() + ", service ip: " + host.getServiceIP() + ", version: " + host.getVersion());
        	}

        	List<ManagementServerHostVO> downHostList = new ArrayList<ManagementServerHostVO>();
            for(ManagementServerHostVO host : inactiveList) {
	            if(!pingManagementNode(host)) {
	                s_logger.warn("Management node " + host.getId() + " is detected inactive by timestamp and also not pingable");
	                downHostList.add(host);	
	            }
            }
            
            if(downHostList.size() > 0)
            	this.queueNotification(new ClusterManagerMessage(ClusterManagerMessage.MessageType.nodeRemoved, downHostList));
        } else {
        	s_logger.info("No inactive management server node found");
        }
    }

    private void peerScan() throws ActiveFencingException {
        Date cutTime = DateUtil.currentGMTTime();

        Profiler profiler = new Profiler();
        profiler.start();
        
        Profiler profilerQueryActiveList = new Profiler();
        profilerQueryActiveList.start();
        List<ManagementServerHostVO> currentList = _mshostDao.getActiveList(new Date(cutTime.getTime() - _heartbeatThreshold));
        profilerQueryActiveList.stop();

        Profiler profilerSyncClusterInfo = new Profiler();
        profilerSyncClusterInfo.start();
        List<ManagementServerHostVO> removedNodeList = new ArrayList<ManagementServerHostVO>();
        List<ManagementServerHostVO> invalidatedNodeList = new ArrayList<ManagementServerHostVO>();

        if(_mshostId != null) {
            
            if(_mshostPeerDao.countStateSeenInPeers(_mshostId, _runId, ManagementServerHost.State.Down) > 0) {
                String msg = "We have detected that at least one management server peer reports that this management server is down, perform active fencing to avoid split-brain situation";
                s_logger.error(msg);
                throw new ActiveFencingException(msg);
            }
            
            // only if we have already attached to cluster, will we start to check leaving nodes
            for(Map.Entry<Long, ManagementServerHostVO>  entry : _activePeers.entrySet()) {

                ManagementServerHostVO current = getInListById(entry.getKey(), currentList);
                if(current == null) {
                    if(entry.getKey().longValue() != _mshostId.longValue()) {
                        if(s_logger.isDebugEnabled()) {
                            s_logger.debug("Detected management node left, id:" + entry.getKey() + ", nodeIP:" + entry.getValue().getServiceIP());
                        }
                        removedNodeList.add(entry.getValue());
                    }
                } else {
                    if(current.getRunid() == 0) {
                        if(entry.getKey().longValue() != _mshostId.longValue()) {
                            if(s_logger.isDebugEnabled()) {
                                s_logger.debug("Detected management node left because of invalidated session, id:" + entry.getKey() + ", nodeIP:" + entry.getValue().getServiceIP());
                            }
                            invalidatedNodeList.add(entry.getValue());
                        }
                    } else {
                        if(entry.getValue().getRunid() != current.getRunid()) {
                            if(s_logger.isDebugEnabled()) {
                                s_logger.debug("Detected management node left and rejoined quickly, id:" + entry.getKey() + ", nodeIP:" + entry.getValue().getServiceIP());
                            }

                            entry.getValue().setRunid(current.getRunid());
                        }
                    }
                }
            }
        }
        profilerSyncClusterInfo.stop();
        
        Profiler profilerInvalidatedNodeList = new Profiler();
        profilerInvalidatedNodeList.start();
        // process invalidated node list
        if(invalidatedNodeList.size() > 0) {
            for(ManagementServerHostVO mshost : invalidatedNodeList) {
                _activePeers.remove(mshost.getId());
                try {
                    JmxUtil.unregisterMBean("ClusterManager", "Node " + mshost.getId());
                } catch(Exception e) {
                    s_logger.warn("Unable to deregiester cluster node from JMX monitoring due to exception " + e.toString());
                }
            }

            this.queueNotification(new ClusterManagerMessage(ClusterManagerMessage.MessageType.nodeRemoved, invalidatedNodeList));
        }
        profilerInvalidatedNodeList.stop();

        Profiler profilerRemovedList = new Profiler();
        profilerRemovedList.start();
        // process removed node list
        Iterator<ManagementServerHostVO> it = removedNodeList.iterator();
        while(it.hasNext()) {
            ManagementServerHostVO mshost = it.next();
            if(!pingManagementNode(mshost)) {
                s_logger.warn("Management node " + mshost.getId() + " is detected inactive by timestamp and also not pingable");
                _activePeers.remove(mshost.getId());
                try {
                    JmxUtil.unregisterMBean("ClusterManager", "Node " + mshost.getId());
                } catch(Exception e) {
                    s_logger.warn("Unable to deregiester cluster node from JMX monitoring due to exception " + e.toString());
                }
            } else {
                s_logger.info("Management node " + mshost.getId() + " is detected inactive by timestamp but is pingable");
                it.remove();
            }
        }

        if(removedNodeList.size() > 0) {
            this.queueNotification(new ClusterManagerMessage(ClusterManagerMessage.MessageType.nodeRemoved, removedNodeList));
        }
        profilerRemovedList.stop();

        List<ManagementServerHostVO> newNodeList = new ArrayList<ManagementServerHostVO>();
        for(ManagementServerHostVO mshost : currentList) {
            if(!_activePeers.containsKey(mshost.getId())) {
                _activePeers.put(mshost.getId(), mshost);

                if(s_logger.isDebugEnabled()) {
                    s_logger.debug("Detected management node joined, id:" + mshost.getId() + ", nodeIP:" + mshost.getServiceIP());
                }
                newNodeList.add(mshost);

                try {
                    JmxUtil.registerMBean("ClusterManager", "Node " + mshost.getId(), new ClusterManagerMBeanImpl(this, mshost));
                } catch(Exception e) {
                    s_logger.warn("Unable to regiester cluster node into JMX monitoring due to exception " + ExceptionUtil.toString(e));
                }
            }
        }

        if(newNodeList.size() > 0) {
            this.queueNotification(new ClusterManagerMessage(ClusterManagerMessage.MessageType.nodeAdded, newNodeList));
        }
        
        profiler.stop();
        
        if(profiler.getDuration() >= this._heartbeatInterval) {
            if(s_logger.isDebugEnabled())
                s_logger.debug("Peer scan takes too long to finish. profiler: " + profiler.toString()
                  + ", profilerQueryActiveList: " + profilerQueryActiveList.toString()
                  + ", profilerSyncClusterInfo: " + profilerSyncClusterInfo.toString()
                  + ", profilerInvalidatedNodeList: " + profilerInvalidatedNodeList.toString()
                  + ", profilerRemovedList: " + profilerRemovedList.toString());
        }
    }

    private static ManagementServerHostVO getInListById(Long id, List<ManagementServerHostVO> l) {
        for(ManagementServerHostVO mshost : l) {
            if(mshost.getId() == id) {
                return mshost;
            }
        }
        return null;
    }

    @Override @DB
    public boolean start() {
        if(s_logger.isInfoEnabled()) {
            s_logger.info("Starting cluster manager, msid : " + _msId);
        }

        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();

            final Class<?> c = this.getClass();
            String version = c.getPackage().getImplementationVersion();

            ManagementServerHostVO mshost = _mshostDao.findByMsid(_msId);
            if (mshost == null) {
                mshost = new ManagementServerHostVO();
                mshost.setMsid(_msId);
                mshost.setRunid(this.getCurrentRunId());
                mshost.setName(NetUtils.getHostName());
                mshost.setVersion(version);
                mshost.setServiceIP(_clusterNodeIP);
                mshost.setServicePort(_currentServiceAdapter.getServicePort());
                mshost.setLastUpdateTime(DateUtil.currentGMTTime());
                mshost.setRemoved(null);
                mshost.setAlertCount(0);
                mshost.setState(ManagementServerHost.State.Up);
                _mshostDao.persist(mshost);

                if (s_logger.isInfoEnabled()) {
                    s_logger.info("New instance of management server msid " + _msId + " is being started");
                }
            } else {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Management server " + _msId + " is being started");
                }

                _mshostDao.update(mshost.getId(), getCurrentRunId(), NetUtils.getHostName(), version, _clusterNodeIP, _currentServiceAdapter.getServicePort(), DateUtil.currentGMTTime());
            }

            txn.commit();

            _mshostId = mshost.getId();
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Management server (host id : " + _mshostId + ") is being started at " + _clusterNodeIP + ":" + _currentServiceAdapter.getServicePort());
            }
            
            _mshostPeerDao.clearPeerInfo(_mshostId);

            // use seperate thread for heartbeat updates
            _heartbeatScheduler.scheduleAtFixedRate(getHeartbeatTask(), _heartbeatInterval, _heartbeatInterval, TimeUnit.MILLISECONDS);
            _notificationExecutor.submit(getNotificationTask());

        } catch (Throwable e) {
            s_logger.error("Unexpected exception : ", e);
            txn.rollback();

            throw new CloudRuntimeException("Unable to initialize cluster info into database");
        }

        if (s_logger.isInfoEnabled()) {
            s_logger.info("Cluster manager was started successfully");
        }

        return true;
    }

    @Override @DB
    public boolean stop() {
        if(_mshostId != null) {
            ManagementServerHostVO mshost = _mshostDao.findByMsid(_msId);
            mshost.setState(ManagementServerHost.State.Down);
            _mshostDao.update(_mshostId, mshost);
        }

        _heartbeatScheduler.shutdownNow();
        _executor.shutdownNow();

        try {
            _heartbeatScheduler.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS);
            _executor.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }

        if(s_logger.isInfoEnabled()) {
            s_logger.info("Cluster manager is stopped");
        }

        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        if(s_logger.isInfoEnabled()) {
            s_logger.info("Start configuring cluster manager : " + name);
        }

        Map<String, String> configs = _configDao.getConfiguration("management-server", params);

        String value = configs.get("cluster.heartbeat.interval");
        if (value != null) {
            _heartbeatInterval = NumbersUtil.parseInt(value, ClusterManager.DEFAULT_HEARTBEAT_INTERVAL);
        }

        value = configs.get("cluster.heartbeat.threshold");
        if (value != null) {
            _heartbeatThreshold = NumbersUtil.parseInt(value, ClusterManager.DEFAULT_HEARTBEAT_THRESHOLD);
        }

        File dbPropsFile = PropertiesUtil.findConfigFile("db.properties");
        Properties dbProps = new Properties();
        try {
            dbProps.load(new FileInputStream(dbPropsFile));
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Unable to find db.properties");
        } catch (IOException e) {
            throw new ConfigurationException("Unable to load db.properties content");
        }
        _clusterNodeIP = dbProps.getProperty("cluster.node.IP");
        if (_clusterNodeIP == null) {
            _clusterNodeIP = "127.0.0.1";
        }
        _clusterNodeIP = _clusterNodeIP.trim();

        if(s_logger.isInfoEnabled()) {
            s_logger.info("Cluster node IP : " + _clusterNodeIP);
        }

        if(!NetUtils.isLocalAddress(_clusterNodeIP)) {
            throw new ConfigurationException("cluster node IP should be valid local address where the server is running, please check your configuration");
        }

        for(int i = 0; i < DEFAULT_OUTGOING_WORKERS; i++)
        	_executor.execute(getClusterPduSendingTask());
        
        // notification task itself in turn works as a task dispatcher
        _executor.execute(getClusterPduNotificationTask());

        if (_serviceAdapters == null) {
            throw new ConfigurationException("Unable to get cluster service adapters");
        }
        _currentServiceAdapter = _serviceAdapters.get(0);

        if(_currentServiceAdapter == null) {
            throw new ConfigurationException("Unable to set current cluster service adapter");
        }

        _agentLBEnabled = Boolean.valueOf(_configDao.getValue(Config.AgentLbEnable.key()));
        
        String connectedAgentsThreshold = configs.get("agent.load.threshold");
        
        if (connectedAgentsThreshold != null) {
            _connectedAgentsThreshold = Double.parseDouble(connectedAgentsThreshold);
        }

        this.registerListener(new LockMasterListener(_msId));

        checkConflicts();

        if(s_logger.isInfoEnabled()) {
            s_logger.info("Cluster manager is configured.");
        }
        return true;
    }

    @Override
    public long getManagementNodeId() {
        return _msId;
    }

    @Override
    public long getCurrentRunId() {
        return _runId;
    }

    @Override
    public boolean isManagementNodeAlive(long msid) {
        ManagementServerHostVO mshost = _mshostDao.findByMsid(msid);
        if(mshost != null) {
            if(mshost.getLastUpdateTime().getTime() >=  DateUtil.currentGMTTime().getTime() - _heartbeatThreshold) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean pingManagementNode(long msid) {
        ManagementServerHostVO mshost = _mshostDao.findByMsid(msid);
        if(mshost == null) {
            return false;
        }

        return pingManagementNode(mshost);
    }

    private boolean pingManagementNode(ManagementServerHostVO mshost) {

        String targetIp = mshost.getServiceIP();
        if("127.0.0.1".equals(targetIp) || "0.0.0.0".equals(targetIp)) {
            s_logger.info("ping management node cluster service can not be performed on self");
            return false;
        }
     
        int retry = 10;
        while (--retry > 0) {
            SocketChannel sch = null;
            try {
                s_logger.info("Trying to connect to " + targetIp);
                sch = SocketChannel.open();
                sch.configureBlocking(true);
                sch.socket().setSoTimeout(5000);

                InetSocketAddress addr = new InetSocketAddress(targetIp, mshost.getServicePort());
                sch.connect(addr);
                return true;
            } catch (IOException e) {
                if (e instanceof ConnectException) {
                    s_logger.error("Unable to ping management server at " + targetIp + ":" + mshost.getServicePort() + " due to ConnectException", e);
                	return false;
                }
            } finally {
                if (sch != null) {
                    try {
                        sch.close();
                    } catch (IOException e) {
                    }
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }
        }
        
        s_logger.error("Unable to ping management server at " + targetIp + ":" + mshost.getServicePort() + " after retries");
        return false;
    }


    @Override
    public int getHeartbeatThreshold() {
        return this._heartbeatThreshold;
    }

    public int getHeartbeatInterval() {
        return this._heartbeatInterval;
    }

    public void setHeartbeatThreshold(int threshold) {
        _heartbeatThreshold = threshold;
    }

    private void checkConflicts() throws ConfigurationException {
        Date cutTime = DateUtil.currentGMTTime();
        List<ManagementServerHostVO> peers = _mshostDao.getActiveList(new Date(cutTime.getTime() - _heartbeatThreshold));
        for(ManagementServerHostVO peer : peers) {
            String peerIP = peer.getServiceIP().trim();
            if(_clusterNodeIP.equals(peerIP)) {
                if("127.0.0.1".equals(_clusterNodeIP)) {
                    if(pingManagementNode(peer.getMsid())) {
                        String msg = "Detected another management node with localhost IP is already running, please check your cluster configuration";
                        s_logger.error(msg);
                        throw new ConfigurationException(msg);
                    } else {
                        String msg = "Detected another management node with localhost IP is considered as running in DB, however it is not pingable, we will continue cluster initialization with this management server node";
                        s_logger.info(msg);
                    }
                } else {
                    if(pingManagementNode(peer.getMsid())) {
                        String msg = "Detected that another management node with the same IP " + peer.getServiceIP() + " is already running, please check your cluster configuration";
                        s_logger.error(msg);
                        throw new ConfigurationException(msg);
                    } else {
                        String msg = "Detected that another management node with the same IP " + peer.getServiceIP()
                                + " is considered as running in DB, however it is not pingable, we will continue cluster initialization with this management server node";
                        s_logger.info(msg);
                    }
                }
            }
        }
    }

    @Override
    public boolean rebalanceAgent(long agentId, Event event, long currentOwnerId, long futureOwnerId) throws AgentUnavailableException, OperationTimedoutException {
        return _rebalanceService.executeRebalanceRequest(agentId, currentOwnerId, futureOwnerId, event);
    }

    @Override
    public  boolean isAgentRebalanceEnabled() {
        return _agentLBEnabled;
    }
    
    @Override
    public Boolean propagateResourceEvent(long agentId, ResourceState.Event event) throws AgentUnavailableException {
        final String msPeer = getPeerName(agentId);
        if (msPeer == null) {
            return null;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Propagating agent change request event:" + event.toString() + " to agent:" + agentId);
        }
        Command[] cmds = new Command[1];
        cmds[0] = new PropagateResourceEventCommand(agentId, event);

        Answer[] answers = execute(msPeer, agentId, cmds, true);
        if (answers == null) {
            throw new AgentUnavailableException(agentId);
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Result for agent change is " + answers[0].getResult());
        }

        return answers[0].getResult();
    }
    
    @Override
    public boolean executeResourceUserRequest(long hostId, ResourceState.Event event) throws AgentUnavailableException {
        return _resourceMgr.executeUserRequest(hostId, event);
    }
}
