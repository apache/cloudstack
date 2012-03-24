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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Status.Event;
import com.cloud.resource.ResourceState;
import com.cloud.utils.component.Manager;

public interface ClusterManager extends Manager {
	public static final int DEFAULT_HEARTBEAT_INTERVAL = 1500;
	public static final int DEFAULT_HEARTBEAT_THRESHOLD = 150000;
	public static final String ALERT_SUBJECT = "cluster-alert";
	
	public void OnReceiveClusterServicePdu(ClusterServicePdu pdu);
    public void executeAsync(String strPeer, long agentId, Command [] cmds, boolean stopOnError);
    public Answer[] execute(String strPeer, long agentId, Command [] cmds, boolean stopOnError);

    public Answer[] sendToAgent(Long hostId, Command []  cmds, boolean stopOnError) throws AgentUnavailableException, OperationTimedoutException;
    public boolean executeAgentUserRequest(long agentId, Event event) throws AgentUnavailableException;
    public Boolean propagateAgentEvent(long agentId, Event event) throws AgentUnavailableException;
    public Boolean propagateResourceEvent(long agentId, ResourceState.Event event) throws AgentUnavailableException;
    public boolean executeResourceUserRequest(long hostId, ResourceState.Event event) throws AgentUnavailableException;
	
	public int getHeartbeatThreshold();
	
	public long getManagementNodeId();		// msid of current management server node
    public boolean isManagementNodeAlive(long msid);
    public boolean pingManagementNode(long msid);
	public long getCurrentRunId();
    
	public String getSelfPeerName();
	public String getSelfNodeIP();
    public String getPeerName(long agentHostId);
	
	public void registerListener(ClusterManagerListener listener);
	public void unregisterListener(ClusterManagerListener listener);
    public ManagementServerHostVO getPeer(String peerName);
    
    /**
     * Broadcast the command to all of the  management server nodes.
     * @param agentId agent id this broadcast is regarding
     * @param cmds commands to broadcast
     */
    public void broadcast(long agentId, Command[] cmds);
    
    boolean rebalanceAgent(long agentId, Event event, long currentOwnerId, long futureOwnerId) throws AgentUnavailableException, OperationTimedoutException;
    
    boolean isAgentRebalanceEnabled();
}
