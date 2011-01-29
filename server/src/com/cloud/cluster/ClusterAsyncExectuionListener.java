package com.cloud.cluster;

import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.host.HostVO;
import com.cloud.host.Status;

public class ClusterAsyncExectuionListener implements Listener {
	private final ClusterManager clusterMgr;
	private final String callingPeer;
	private boolean recurring = false;
	
	public ClusterAsyncExectuionListener(ClusterManager clusterMgr, String callingPeer) {
		this.clusterMgr = clusterMgr;
		this.callingPeer = callingPeer;
	}
	
    @Override
    public boolean processAnswers(long agentId, long seq, Answer[] answers) {
    	recurring = clusterMgr.forwardAnswer(callingPeer, agentId, seq, answers);
    	return true;
    }
    
    @Override
    public boolean processCommands(long agentId, long seq, Command[] commands) {
    	return false;
    }
    
    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
    	return null;
    }
    
    @Override
    public void processConnect(HostVO agent, StartupCommand cmd) {
    }
    
    @Override
    public boolean processDisconnect(long agentId, Status state) {
    	return false;
    }
    
    @Override
    public boolean isRecurring() {
    	return recurring;
    }
    
    @Override
    public boolean processTimeout(long agentId, long seq) {
    	return true;
    }
    
    @Override
    public int getTimeout() {
    	return -1;
    }
    
}

