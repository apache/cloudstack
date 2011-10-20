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
package com.cloud.agent;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.manager.AgentAttache;
import com.cloud.agent.manager.Commands;
import com.cloud.api.commands.UpdateHostPasswordCmd;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.PodCluster;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostStats;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.Status.Event;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ServerResource;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;

/**
 * AgentManager manages hosts. It directly coordinates between the DAOs and the connections it manages.
 */
public interface AgentManager extends Manager {
    public enum OnError {
        Continue, Stop
    }

    public enum TapAgentsAction {
        Add,
        Del,
        Contains,
    }
    
    /**
     * easy send method that returns null if there's any errors. It handles all exceptions.
     * 
     * @param hostId
     *            host id
     * @param cmd
     *            command to send.
     * @return Answer if successful; null if not.
     */
    Answer easySend(Long hostId, Command cmd);

    /**
     * Synchronous sending a command to the agent.
     * 
     * @param hostId
     *            id of the agent on host
     * @param cmd
     *            command
     * @return an Answer
     */

    Answer send(Long hostId, Command cmd) throws AgentUnavailableException, OperationTimedoutException;

    /**
     * Synchronous sending a list of commands to the agent.
     * 
     * @param hostId
     *            id of the agent on host
     * @param cmds
     *            array of commands
     * @param isControl
     *            Commands sent contains control commands
     * @param stopOnError
     *            should the agent stop execution on the first error.
     * @return an array of Answer
     */
    Answer[] send(Long hostId, Commands cmds) throws AgentUnavailableException, OperationTimedoutException;

    Answer[] send(Long hostId, Commands cmds, int timeout) throws AgentUnavailableException, OperationTimedoutException;

    /**
     * Asynchronous sending of a command to the agent.
     * 
     * @param hostId
     *            id of the agent on the host.
     * @param cmd
     *            Command to send.
     * @param listener
     *            the listener to process the answer.
     * @return sequence number.
     */
    long gatherStats(Long hostId, Command cmd, Listener listener);

    /**
     * Asynchronous sending of a command to the agent.
     * 
     * @param hostId
     *            id of the agent on the host.
     * @param cmds
     *            Commands to send.
     * @param stopOnError
     *            should the agent stop execution on the first error.
     * @param listener
     *            the listener to process the answer.
     * @return sequence number.
     */
    long send(Long hostId, Commands cmds, Listener listener) throws AgentUnavailableException;

    /**
     * Register to listen for host events. These are mostly connection and disconnection events.
     * 
     * @param listener
     * @param connections
     *            listen for connections
     * @param commands
     *            listen for connections
     * @param priority
     *            in listening for events.
     * @return id to unregister if needed.
     */
    int registerForHostEvents(Listener listener, boolean connections, boolean commands, boolean priority);


    /**
     * Register to listen for initial agent connections.
     * @param creator
     * @param priority in listening for events.
     * @return id to unregister if needed.
     */
    int registerForInitialConnects(StartupCommandProcessor creator,  boolean priority);

    /**
     * Unregister for listening to host events.
     * 
     * @param id
     *            returned from registerForHostEvents
     */
    void unregisterForHostEvents(int id);

    /**
     * @return hosts currently connected.
     */
    Set<Long> getConnectedHosts();
    
    HostStats getHostStatistics(long hostId);

    Long getGuestOSCategoryId(long hostId);

    String getHostTags(long hostId);

    List<PodCluster> listByDataCenter(long dcId);

    List<PodCluster> listByPod(long podId);

    /**
     * Find a pod based on the user id, template, and data center.
     * 
     * @param template
     * @param dc
     * @param userId
     * @return
     */
    Pair<HostPodVO, Long> findPod(VirtualMachineTemplate template, ServiceOfferingVO offering, DataCenterVO dc, long userId, Set<Long> avoids);

    public boolean executeUserRequest(long hostId, Event event) throws AgentUnavailableException;

    boolean isHostNativeHAEnabled(long hostId);

    Answer sendTo(Long dcId, HypervisorType type, Command cmd);

    void notifyAnswersToMonitors(long agentId, long seq, Answer[] answers);

    long sendToSecStorage(HostVO ssHost, Command cmd, Listener listener);

    Answer sendToSecStorage(HostVO ssHost, Command cmd);

    HostVO getSSAgent(HostVO ssHost);
    
    /* working as a lock while agent is being loaded */
    public boolean tapLoadingAgents(Long hostId, TapAgentsAction action);
    
    public AgentAttache handleDirectConnectAgent(HostVO host, StartupCommand[] cmds, ServerResource resource, boolean forRebalance) throws ConnectionException;
    
    public boolean agentStatusTransitTo(HostVO host, Status.Event e, long msId);
    
    public AgentAttache findAttache(long hostId);
    
    void disconnectWithoutInvestigation(long hostId, Status.Event event);
    
    void disconnectWithInvestigation(long hostId, Status.Event event);
    
    public boolean disconnectAgent(HostVO host, Status.Event e, long msId);
    
    public void pullAgentToMaintenance(long hostId);
    
    public void pullAgentOutMaintenance(long hostId);

	boolean reconnect(long hostId);
}
