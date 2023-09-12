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
package com.cloud.agent;

import java.util.Map;

import org.apache.cloudstack.framework.config.ConfigKey;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.manager.Commands;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ServerResource;

/**
 * AgentManager manages hosts. It directly coordinates between the DAOs and the connections it manages.
 */
public interface AgentManager {
    static final ConfigKey<Integer> Wait = new ConfigKey<Integer>("Advanced", Integer.class, "wait", "1800", "Time in seconds to wait for control commands to return",
            true);
    ConfigKey<Boolean> EnableKVMAutoEnableDisable = new ConfigKey<>(Boolean.class,
                    "enable.kvm.host.auto.enable.disable",
                    "Advanced",
                    "false",
                    "(KVM only) Enable Auto Disable/Enable KVM hosts in the cluster " +
                            "according to the hosts health check results",
                    true, ConfigKey.Scope.Cluster, null);

    public enum TapAgentsAction {
        Add, Del, Contains,
    }

    boolean handleDirectConnectAgent(Host host, StartupCommand[] cmds, ServerResource resource, boolean forRebalance, boolean newHost) throws ConnectionException;

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
    int registerForInitialConnects(StartupCommandProcessor creator, boolean priority);

    /**
     * Unregister for listening to host events.
     *
     * @param id
     *            returned from registerForHostEvents
     */
    void unregisterForHostEvents(int id);

    Answer sendTo(Long dcId, HypervisorType type, Command cmd);

    public boolean agentStatusTransitTo(HostVO host, Status.Event e, long msId);

    boolean isAgentAttached(long hostId);

    void disconnectWithoutInvestigation(long hostId, Status.Event event);

    void disconnectWithInvestigation(long hostId, Status.Event event);

    public void pullAgentToMaintenance(long hostId);

    public void pullAgentOutMaintenance(long hostId);

    void reconnect(long hostId) throws AgentUnavailableException;

    void rescan();

    void notifyMonitorsOfNewlyAddedHost(long hostId);

    void notifyMonitorsOfHostAboutToBeRemoved(long hostId);

    void notifyMonitorsOfRemovedHost(long hostId, long clusterId);

    void propagateChangeToAgents(Map<String, String> params);
}
