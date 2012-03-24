/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.cluster;

import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.Listener;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Status.Event;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.MacAddress;

@Local(value={ClusterManager.class})
public class DummyClusterManagerImpl implements ClusterManager {
    private static final Logger s_logger = Logger.getLogger(DummyClusterManagerImpl.class);
	
    protected long _id = MacAddress.getMacAddress().toLong();
    protected long _runId = System.currentTimeMillis();
    
    private String _name;
    private final String _clusterNodeIP = "127.0.0.1";

    @Override
    public void OnReceiveClusterServicePdu(ClusterServicePdu pdu) {
        throw new CloudRuntimeException("Unsupported feature");
    }

    @Override
    public void executeAsync(String strPeer, long agentId, Command [] cmds, boolean stopOnError) {
    	throw new CloudRuntimeException("Unsupported feature");
    }
    
    @Override
    public Answer[] execute(String strPeer, long agentId, Command [] cmds, boolean stopOnError) {
    	throw new CloudRuntimeException("Unsupported feature");
    }
  
    @Override
    public Answer[] sendToAgent(Long hostId, Command []  cmds, boolean stopOnError)
    	throws AgentUnavailableException, OperationTimedoutException {
    	throw new CloudRuntimeException("Unsupported feature");
    }
    
/*    
    @Override
    public long sendToAgent(Long hostId, Command[] cmds, boolean stopOnError, Listener listener) throws AgentUnavailableException {
    	throw new CloudRuntimeException("Unsupported feature");
    }
*/    
    @Override
    public boolean executeAgentUserRequest(long agentId, Event event) throws AgentUnavailableException {
    	throw new CloudRuntimeException("Unsupported feature");
    }
    
    @Override
    public Boolean propagateAgentEvent(long agentId, Event event) throws AgentUnavailableException {
    	throw new CloudRuntimeException("Unsupported feature");
    }
	
	@Override
    public int getHeartbeatThreshold() {
    	return ClusterManager.DEFAULT_HEARTBEAT_INTERVAL;
	}
	
	@Override
    public long getManagementNodeId() {
        return _id;
	}
	
    @Override
    public long getCurrentRunId() {
        return _runId;
    }
	
	@Override
	public ManagementServerHostVO getPeer(String str) {
		return null;
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
    public boolean isManagementNodeAlive(long msid) {
    	return true;
    }
    
    @Override
    public boolean pingManagementNode(long msid) {
    	return false;
    }
	
    @Override
    public String getPeerName(long agentHostId) {
    	throw new CloudRuntimeException("Unsupported feature");
    }
	
	@Override
    public void registerListener(ClusterManagerListener listener) {
	}
	
	@Override
    public void unregisterListener(ClusterManagerListener listener) {
	}

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		return true;
	}
	
	@Override
	public void broadcast(long hostId, Command[] cmds) {
	}

	@Override
	public String getName() {
        return _name;
	}

	@Override
	public boolean start() {
    	if(s_logger.isInfoEnabled())
    		s_logger.info("Starting cluster manager, msid : " + _id);
    	
        return true;
	}

	@Override
	public boolean stop() {
		return true;
	}
	
	@Override
	public boolean rebalanceAgent(long agentId, Event event, long currentOwnerId, long futureOwnerId) throws AgentUnavailableException, OperationTimedoutException {
	    return false;
	}
	
	@Override
    public  boolean isAgentRebalanceEnabled() {
        return false;
    }

	@Override
    public Boolean propagateResourceEvent(long agentId, com.cloud.resource.ResourceState.Event event) throws AgentUnavailableException {
	    // TODO Auto-generated method stub
	    return null;
    }

	@Override
    public boolean executeResourceUserRequest(long hostId, com.cloud.resource.ResourceState.Event event) throws AgentUnavailableException {
	    // TODO Auto-generated method stub
	    return false;
    }
}
