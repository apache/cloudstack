package com.cloud.cluster;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.AgentManager.OnError;
import com.cloud.agent.Listener;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ChangeAgentCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.manager.Commands;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.HostVO;
import com.cloud.host.Status.Event;
import com.cloud.host.dao.HostDao;
import com.cloud.serializer.GsonHelper;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.events.SubscriptionMgr;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.MacAddress;
import com.cloud.utils.net.NetUtils;
import com.google.gson.Gson;

@Local(value={ClusterManager.class})
public class ClusterManagerImpl implements ClusterManager {
    private static final Logger s_logger = Logger.getLogger(ClusterManagerImpl.class);

    private static final int EXECUTOR_SHUTDOWN_TIMEOUT = 1000;					// 1 second
    
    private final List<ClusterManagerListener> listeners = new ArrayList<ClusterManagerListener>();
    private final Map<Long, ManagementServerHostVO> activePeers = new HashMap<Long, ManagementServerHostVO>();
    private int heartbeatInterval = ClusterManager.DEFAULT_HEARTBEAT_INTERVAL;
    private int heartbeatThreshold = ClusterManager.DEFAULT_HEARTBEAT_THRESHOLD;
    
    private final Map<String, ClusterService> clusterPeers;
    private final Map<String, Listener> asyncCalls;
    private final Gson gson;
    
    private AgentManager _agentMgr;
    
    private final ScheduledExecutorService _heartbeatScheduler =
        Executors.newScheduledThreadPool(1, new NamedThreadFactory("Cluster-Heartbeat"));
    
    private final ExecutorService _executor;
    
    private ClusterServiceAdapter _currentServiceAdapter;
    
    private ManagementServerHostDao _mshostDao;
    private HostDao _hostDao;
    
    protected long _id = MacAddress.getMacAddress().toLong();
    
    private Long _mshostId = null;
    
    private String _name;
    private String _clusterNodeIP = "127.0.0.1";
	
    public ClusterManagerImpl() {
    	clusterPeers = new HashMap<String, ClusterService>();
    	asyncCalls = new HashMap<String, Listener>();
    	
		gson = GsonHelper.getBuilder().create();
		
		// executor to perform remote-calls in another thread context, to avoid potential
		// recursive remote calls between nodes
		//
		_executor = Executors.newCachedThreadPool(new NamedThreadFactory("Cluster-Worker"));
    }
    
    @Override
    public Answer[] sendToAgent(Long hostId, Command []  cmds, boolean stopOnError)
    	throws AgentUnavailableException, OperationTimedoutException {
        Commands commands = new Commands(stopOnError ? OnError.Stop : OnError.Continue);
        for (Command cmd  : cmds) {
            commands.addCommand(cmd);
        }
    	return _agentMgr.send(hostId, commands);
    }
    	
    @Override
    public long sendToAgent(Long hostId, Command[] cmds, boolean stopOnError, Listener listener)
    	throws AgentUnavailableException {
        Commands commands = new Commands(stopOnError ? OnError.Stop : OnError.Continue);
        for (Command cmd  : cmds) {
            commands.addCommand(cmd);
        }
    	return _agentMgr.send(hostId, commands, listener);
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
    		s_logger.debug("Propagating agent change request event:" + event.toString() + " to agent:"+ agentId);
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
    
    @Override
    public void broadcast(long agentId, Command[] cmds) {
    	List<ManagementServerHostVO> peers = _mshostDao.getActiveList(new Date());
    	for (ManagementServerHostVO peer : peers) {
    		String peerName = Long.toString(peer.getMsid());
    		if (getSelfPeerName().equals(peerName)) {
    			continue;	// Skip myself.
    		}
    		try {
    			if (s_logger.isDebugEnabled()) {
    				s_logger.debug("Forwarding " + cmds[0].toString() + " to " + peer.getMsid());
    			}
    			Answer[] answers = execute(peerName, agentId, cmds, true);
    		} catch (Exception e) {
    			s_logger.warn("Caught exception while talkign to " + peer.getMsid());
    		}
    	}
    }
    
    @Override
    public Answer[] execute(String strPeer, long agentId, Command [] cmds, boolean stopOnError) {
    	ClusterService peerService =  null;
    	
    	if(s_logger.isDebugEnabled()) {
            s_logger.debug(getSelfPeerName() + " -> " + strPeer + "." + agentId + " " +
    			gson.toJson(cmds, Command[].class));
        }
    	
    	for(int i = 0; i < 2; i++) {
	    	try {
				peerService = getPeerService(strPeer);
			} catch (RemoteException e) {
				s_logger.error("Unable to get cluster service on peer : " + strPeer);
			}
			
			if(peerService != null) {
				try {
			    	if(s_logger.isDebugEnabled()) {
                        s_logger.debug("Send " + getSelfPeerName() + " -> " + strPeer + "." + agentId + " to remote");
                    }
					
			    	long startTick = System.currentTimeMillis();
					String strResult = peerService.execute(getSelfPeerName(), agentId, gson.toJson(cmds, Command[].class), stopOnError);
					if(s_logger.isDebugEnabled()) {
                        s_logger.debug("Completed " + getSelfPeerName() + " -> " + strPeer + "." + agentId + "in " +
							(System.currentTimeMillis() - startTick) + " ms, result: " + strResult);
                    }
					
					if(strResult != null) {
						try {
							return gson.fromJson(strResult, Answer[].class);
						} catch(Throwable e) {
							s_logger.error("Exception on parsing gson package from remote call to " + strPeer);
						}
					}
					
					return null;
				} catch (RemoteException e) {
					invalidatePeerService(strPeer);
					
					if(s_logger.isInfoEnabled()) {
                        s_logger.info("Exception on remote execution, peer: " + strPeer + ", iteration: "
							+ i + ", exception message :" + e.getMessage());
                    }
				}
			}
    	}
    	
    	return null;
    }
    
    @Override
    public long executeAsync(String strPeer, long agentId, Command[] cmds, boolean stopOnError, Listener listener) {
    	
    	ClusterService peerService =  null;
    	
    	if(s_logger.isDebugEnabled()) {
            s_logger.debug("Async " + getSelfPeerName() + " -> " + strPeer + "." + agentId + " " +
    			gson.toJson(cmds, Command[].class));
        }
    	
    	for(int i = 0; i < 2; i++) {
	    	try {
				peerService = getPeerService(strPeer);
			} catch (RemoteException e) {
				s_logger.error("Unable to get cluster service on peer : " + strPeer);
			}
			
			if(peerService != null) {
				try {
					long seq = 0;
					synchronized(String.valueOf(agentId).intern()) {
				    	if(s_logger.isDebugEnabled()) {
                            s_logger.debug("Send Async " + getSelfPeerName() + " -> " + strPeer + "." + agentId + " to remote");
                        }

				    	long startTick = System.currentTimeMillis();
						seq = peerService.executeAsync(getSelfPeerName(), agentId, gson.toJson(cmds, Command[].class), stopOnError);
						
						if(seq > 0) {
							if(s_logger.isDebugEnabled()) {
                                s_logger.debug("Completed Async " + getSelfPeerName() + " -> " + strPeer + "." + agentId
									+ " in " + (System.currentTimeMillis() - startTick) + " ms"
									+ ", register local listener " + strPeer + "/" + seq);
                            }
							
							registerAsyncCall(strPeer, seq, listener);
						} else {
							s_logger.warn("Completed Async " + getSelfPeerName() + " -> " + strPeer + "." + agentId
									+ " in " + (System.currentTimeMillis() - startTick) + " ms, return indicates failure, seq: " + seq);
						}
					}
					return seq;
				} catch (RemoteException e) {
					invalidatePeerService(strPeer);
					
					if(s_logger.isInfoEnabled()) {
                        s_logger.info("Exception on remote execution -> " + strPeer + ", iteration : " + i);
                    }
				}
			}
    	}
    	
    	return 0L;
    }
    
    @Override
    public boolean onAsyncResult(String executingPeer, long agentId, long seq, Answer[] answers) {
    	
		if(s_logger.isDebugEnabled()) {
            s_logger.debug("Process Async-call result from remote peer " + executingPeer + ", {" +
				agentId + "-" + seq + "} answers: " + (answers != null ? gson.toJson(answers, Answer[].class): "null"));
        }

		Listener listener = null;
		synchronized(String.valueOf(agentId).intern()) {
			// need to synchronize it with executeAsync() to make sure listener have been registered
			// before this callback reaches back
			listener = getAsyncCallListener(executingPeer, seq);
		}
		
		if(listener != null) {
			long startTick = System.currentTimeMillis();
			
			if(s_logger.isDebugEnabled()) {
                s_logger.debug("Processing answer {" + agentId + "-" + seq + "} from remote peer " + executingPeer);
            }
			
			listener.processAnswers(agentId, seq, answers);
			
			if(s_logger.isDebugEnabled()) {
                s_logger.debug("Answer {" + agentId + "-" + seq + "} is processed in " +
					(System.currentTimeMillis() - startTick) + " ms");
            }
			
			if(!listener.isRecurring()) {
				if(s_logger.isDebugEnabled()) {
                    s_logger.debug("Listener is not recurring after async-result callback {" +
						agentId + "-" + seq + "}, unregister it");
                }
				
				unregisterAsyncCall(executingPeer, seq);
			} else {
				if(s_logger.isDebugEnabled()) {
                    s_logger.debug("Listener is recurring after async-result callback {" + agentId
							+"-" + seq + "}, will keep it");
                }
				return true;
			}
		} else {
			if(s_logger.isInfoEnabled()) {
                s_logger.info("Async-call Listener has not been registered yet for {" + agentId
					+"-" + seq + "}");
            }
		}
		return false;
    }
    
    @Override
    public boolean forwardAnswer(String targetPeer, long agentId, long seq, Answer[] answers) {

    	if(s_logger.isDebugEnabled()) {
            s_logger.debug("Forward -> " + targetPeer + " Async-call answer {" + agentId + "-" + seq +
    			"} " + (answers != null? gson.toJson(answers, Answer[].class):""));
        }
    	
    	final String targetPeerF = targetPeer;
    	final Answer[] answersF = answers;
    	final long agentIdF = agentId;
    	final long seqF = seq;
    	
    	ClusterService peerService = null;
    	
    	for(int i = 0; i < 2; i++) {
	    	try {
				peerService = getPeerService(targetPeerF);
			} catch (RemoteException e) {
				s_logger.error("cluster service for peer " + targetPeerF + " no longer exists");
			}
			
	    	if(peerService != null) {
	    		try {
	    			boolean result = false;
	    			
	    			long startTick = System.currentTimeMillis();
	    			if(s_logger.isDebugEnabled()) {
                        s_logger.debug("Start forwarding Async-call answer {" + agentId + "-" + seq + "} to remote");
                    }
	    			
	    			result = peerService.onAsyncResult(getSelfPeerName(), agentIdF, seqF, gson.toJson(answersF, Answer[].class));
	    			
	    			if(s_logger.isDebugEnabled()) {
                        s_logger.debug("Completed forwarding Async-call answer {" + agentId + "-" + seq + "} in " +
	    					(System.currentTimeMillis() - startTick) + " ms, return result: " + result);
                    }
	    			
	    			return result;
				} catch (RemoteException e) {
					s_logger.warn("Exception in performing remote call, ", e);
					invalidatePeerService(targetPeerF);
				}
	    	} else {
	    		s_logger.warn("Remote peer " + targetPeer + " no longer exists to process answer {" + agentId + "-"
	    			+ seq + "}");
	    	}
    	}
    	
    	return false;
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
		return Long.toString(_id);
	}

	@Override
	public String getSelfNodeIP() {
		return _clusterNodeIP;
	}
	
	@Override
	public void registerListener(ClusterManagerListener listener) {
		// Note : we don't check duplicates
		synchronized(listeners) {
			listeners.add(listener);
		}
	}
	
	@Override
	public void unregisterListener(ClusterManagerListener listener) {
		synchronized(listeners) {
			listeners.remove(listener);
		}
	}
	
	public void notifyNodeJoined(List<ManagementServerHostVO> nodeList) {
		synchronized(listeners) {
			for(ClusterManagerListener listener : listeners) {
				listener.onManagementNodeJoined(nodeList, _mshostId);
			}
		}
		
		SubscriptionMgr.getInstance().notifySubscribers(ClusterManager.ALERT_SUBJECT, this,
			new ClusterNodeJoinEventArgs(_mshostId, nodeList));
	}
	
	public void notifyNodeLeft(List<ManagementServerHostVO> nodeList) {
		synchronized(listeners) {
			for(ClusterManagerListener listener : listeners) {
				listener.onManagementNodeLeft(nodeList, _mshostId);
			}
		}
		
		SubscriptionMgr.getInstance().notifySubscribers(ClusterManager.ALERT_SUBJECT, this,
			new ClusterNodeLeftEventArgs(_mshostId, nodeList));
	}
	
	public ClusterService getPeerService(String strPeer) throws RemoteException {
		synchronized(clusterPeers) {
			if(clusterPeers.containsKey(strPeer)) {
                return clusterPeers.get(strPeer);
            }
		}
		
		ClusterService service = _currentServiceAdapter.getPeerService(strPeer);
		
		if(service != null) {
			synchronized(clusterPeers) {
				// re-check the peer map again to deal with the
				// race conditions
				if(!clusterPeers.containsKey(strPeer)) {
                    clusterPeers.put(strPeer, service);
                }
			}
		}
		
		return service;
	}
	
	public void invalidatePeerService(String strPeer) {
		synchronized(clusterPeers) {
			if(clusterPeers.containsKey(strPeer)) {
                clusterPeers.remove(strPeer);
            }
		}
	}
	
	private void registerAsyncCall(String strPeer, long seq, Listener listener) {
		String key = strPeer + "/" + seq;
		
		synchronized(asyncCalls) {
			if(!asyncCalls.containsKey(key)) {
                asyncCalls.put(key, listener);
            }
		}
	}
	
	private Listener getAsyncCallListener(String strPeer, long seq) {
		String key = strPeer + "/" + seq;
		
		synchronized(asyncCalls) {
			if(asyncCalls.containsKey(key)) {
                return asyncCalls.get(key);
            }
		}
		
		return null;
	}
	
	private void unregisterAsyncCall(String strPeer, long seq) {
		String key = strPeer + "/" + seq;

		synchronized(asyncCalls) {
			if(asyncCalls.containsKey(key)) {
                asyncCalls.remove(key);
            }
		}
	}
	
	private Runnable getHeartbeatTask() {
		return new Runnable() {
			@Override
            public void run() {
			    try {
    	    		if(s_logger.isTraceEnabled()) {
                        s_logger.trace("Cluster manager heartbeat update, id:" + _mshostId);
                    }
    	    		
        			_mshostDao.update(_mshostId, DateUtil.currentGMTTime());
        			peerScan();
			    } catch (Exception e) {
			        s_logger.error("Problem with the cluster heartbeat!", e);
			    }
			}
		};
	}
	
	private void peerScan() {
		Date cutTime = DateUtil.currentGMTTime();
		
		List<ManagementServerHostVO> currentList = _mshostDao.getActiveList(new Date(cutTime.getTime() - heartbeatThreshold));

		List<ManagementServerHostVO> removedNodeList = new ArrayList<ManagementServerHostVO>();
		if(_mshostId != null) {
			// only if we have already attached to cluster, will we start to check leaving nodes
			for(Map.Entry<Long, ManagementServerHostVO>  entry : activePeers.entrySet()) {
				if(!isIdInList(entry.getKey(), currentList)) {
					if(entry.getKey().longValue() != _mshostId.longValue()) {
						if(s_logger.isDebugEnabled()) {
                            s_logger.debug("Detected management node left, id:" + entry.getKey() + ", nodeIP:" + entry.getValue().getServiceIP());
                        }
						removedNodeList.add(entry.getValue());
					}
				}
			}
		}
		
		for(ManagementServerHostVO mshost : removedNodeList) {
			activePeers.remove(mshost.getId());
		}
		
		List<ManagementServerHostVO> newNodeList = new ArrayList<ManagementServerHostVO>();
		for(ManagementServerHostVO mshost : currentList) {
			if(!activePeers.containsKey(mshost.getId())) {
				activePeers.put(mshost.getId(), mshost);

				if(s_logger.isDebugEnabled()) {
                    s_logger.debug("Detected management node joined, id:" + mshost.getId() + ", nodeIP:" + mshost.getServiceIP());
                }
				newNodeList.add(mshost);
			}
		}
		
		if(newNodeList.size() > 0) {
            notifyNodeJoined(newNodeList);
        }
		
		if(removedNodeList.size() > 0) {
            notifyNodeLeft(removedNodeList);
        }
	}
	
	private static boolean isIdInList(Long id, List<ManagementServerHostVO> l) {
		for(ManagementServerHostVO mshost : l) {
			if(mshost.getId() != null && mshost.getId() == id) {
                return true;
            }
		}
		return false;
	}
	
    @Override
    public String getName() {
        return _name;
    }
    
    @Override @DB
    public boolean start() {
    	if(s_logger.isInfoEnabled()) {
            s_logger.info("Starting cluster manager, msid : " + _id);
        }
    	
        Transaction txn = Transaction.currentTxn();
        try {
	        txn.start();

	        final Class<?> c = this.getClass();
	        String version = c.getPackage().getImplementationVersion();
	        
	        ManagementServerHostVO mshost = _mshostDao.findByMsid(_id);
	        if(mshost == null) {
	        	mshost = new ManagementServerHostVO();
		        mshost.setMsid(_id);
		        
		        mshost.setName(NetUtils.getHostName());
		        mshost.setVersion(version);
		        mshost.setServiceIP(_clusterNodeIP);
		        mshost.setServicePort(_currentServiceAdapter.getServicePort());
		        mshost.setLastUpdateTime(DateUtil.currentGMTTime());
		        mshost.setRemoved(null);
		        mshost.setAlertCount(0);
		        _mshostDao.persist(mshost);
		        
		        if(s_logger.isInfoEnabled()) {
                    s_logger.info("New instance of management server msid " + _id + " is being started");
                }
	        } else {
		        if(s_logger.isInfoEnabled()) {
                    s_logger.info("Management server " + _id + " is being started");
                }
		        
		        _mshostDao.update(mshost.getId(), NetUtils.getHostName(), version,
		        	_clusterNodeIP, _currentServiceAdapter.getServicePort(), DateUtil.currentGMTTime());
	        }
	        
	        txn.commit();
	        
	        _mshostId = mshost.getId();
	        if(s_logger.isInfoEnabled()) {
                s_logger.info("Management server (host id : " + _mshostId + ") is available at " + _clusterNodeIP + ":" + _currentServiceAdapter.getServicePort());
            }
	        
			_heartbeatScheduler.scheduleAtFixedRate(getHeartbeatTask(), heartbeatInterval,
					heartbeatInterval, TimeUnit.MILLISECONDS);
	        
        } catch (Throwable e) {
        	s_logger.error("Unexpected exception : ", e);
            txn.rollback();
            
            throw new CloudRuntimeException("Unable to initialize cluster info into database");
        }

    	if(s_logger.isInfoEnabled()) {
            s_logger.info("Cluster manager is started");
        }
        return true;
    }

    @Override
    public boolean stop() {
    	if(_mshostId != null) {
            _mshostDao.remove(_mshostId);
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
    	
        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        _agentMgr = locator.getManager(AgentManager.class);
        if (_agentMgr == null) {
            throw new ConfigurationException("Unable to get " + AgentManager.class.getName());
        }
        
        _mshostDao = locator.getDao(ManagementServerHostDao.class);
        if(_mshostDao == null) {
            throw new ConfigurationException("Unable to get " + ManagementServerHostDao.class.getName());
        }
        
        _hostDao = locator.getDao(HostDao.class);
        if(_hostDao == null) {
            throw new ConfigurationException("Unable to get " + HostDao.class.getName());
        }
        
        ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
        if (configDao == null) {
            throw new ConfigurationException("Unable to get the configuration dao.");
        }
        
        Map<String, String> configs = configDao.getConfiguration("management-server", params);
        
        String value = configs.get("cluster.heartbeat.interval");
        if(value != null) {
            heartbeatInterval = NumbersUtil.parseInt(value, ClusterManager.DEFAULT_HEARTBEAT_INTERVAL);
        }
        
        value = configs.get("cluster.heartbeat.threshold");
        if(value != null) {
            heartbeatThreshold = NumbersUtil.parseInt(value, ClusterManager.DEFAULT_HEARTBEAT_THRESHOLD);
        }
    	
        _name = name;

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
        if(_clusterNodeIP == null) {
            _clusterNodeIP = "127.0.0.1";
        }
        if(s_logger.isInfoEnabled()) {
            s_logger.info("Cluster node IP : " + _clusterNodeIP);
        }
        
        if(!NetUtils.isLocalAddress(_clusterNodeIP)) {
            throw new ConfigurationException("cluster node IP should be valid local address where the server is running, please check your configuration");
        }
        
        Adapters<ClusterServiceAdapter> adapters = locator.getAdapters(ClusterServiceAdapter.class);
        if (adapters == null || !adapters.isSet()) {
            throw new ConfigurationException("Unable to get cluster service adapters");
        }
		Enumeration<ClusterServiceAdapter> it = adapters.enumeration();
		if(it.hasMoreElements()) {
            _currentServiceAdapter = it.nextElement();
        }
		
		if(_currentServiceAdapter == null) {
            throw new ConfigurationException("Unable to set current cluster service adapter");
        }
        
        if(s_logger.isInfoEnabled()) {
            s_logger.info("Cluster manager is configured.");
        }
        return true;
    }
    
    @Override
    public long getId() {
        return _id;
    }
    
    @Override
	public int getHeartbeatThreshold() {
    	return this.heartbeatThreshold;
    }
}
