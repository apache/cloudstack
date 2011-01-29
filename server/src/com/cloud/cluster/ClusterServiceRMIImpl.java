package com.cloud.cluster;

import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import com.cloud.agent.Listener;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.cluster.ClusterManager;
import com.cloud.cluster.ClusterService;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.serializer.GsonHelper;
import com.google.gson.Gson;

public class ClusterServiceRMIImpl implements ClusterService {
    private static final Logger s_logger = Logger.getLogger(ClusterServiceRMIImpl.class);
    
    private ClusterManager manager;
    private Gson gson;
    
    public ClusterServiceRMIImpl() {
		gson = GsonHelper.getBuilder().create();
    }
    
    public ClusterServiceRMIImpl(ClusterManager manager) {
    	this.manager = manager;
    	
		gson = GsonHelper.getBuilder().create();
    }
    
    @Override
	public String execute(String callingPeer, long agentId, String gsonPackage, boolean stopOnError) throws RemoteException {
    	
    	if(s_logger.isInfoEnabled())
    		s_logger.info("Execute command forwarded from peer : " + callingPeer);
    	
    	Command [] cmds = null;
    	try {
    		cmds = gson.fromJson(gsonPackage, Command[].class);
    	} catch(Throwable e) {
    		assert(false);
    	}
    	
    	try {
    	Answer[] answers = manager.sendToAgent(agentId, cmds, stopOnError);
    	if(answers != null)
    		return gson.toJson(answers);
    	} catch (AgentUnavailableException e) {
    	    s_logger.warn("Agent unavailable", e);
    	} catch (OperationTimedoutException e) {
    	    s_logger.warn("Timed Out", e);
    	}
		return null;
	}
	
    @Override
	public long executeAsync(String callingPeer, long agentId, String gsonPackage, boolean stopOnError) throws RemoteException {
    	
    	if(s_logger.isInfoEnabled())
    		s_logger.info("Execute Async command forwarded from peer : " + callingPeer);
    	
    	Command [] cmds = null;
    	try {
    		cmds = gson.fromJson(gsonPackage, Command[].class);
    	} catch(Throwable e) {
    		assert(false);
    	}
    	
    	Listener listener = new ClusterAsyncExectuionListener(manager, callingPeer);
    	try {
    	    return manager.sendToAgent(agentId, cmds, stopOnError, listener);
    	} catch (AgentUnavailableException e) {
    	    s_logger.warn("Agent is unavailable", e);
    	    return -1;
    	} 
	}
    
    @Override
	public boolean onAsyncResult(String executingPeer, long agentId, long seq, String gsonPackage) throws RemoteException {
    	Answer[] answers = null;
    	try {
    		answers = gson.fromJson(gsonPackage, Answer[].class);
    	} catch(Throwable e) {
    		assert(false);
    	}
    	
    	return manager.onAsyncResult(executingPeer, agentId, seq, answers);
	}
}

