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

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.management.ManagementServerHost;

import com.cloud.utils.component.Manager;

/**
 * The definition of the framework for inter MS communication.
 */
public interface ClusterManager extends Manager {
    static final String ALERT_SUBJECT = "cluster-alert";
    final ConfigKey<Integer> HeartbeatInterval = new ConfigKey<Integer>(Integer.class, "cluster.heartbeat.interval", "management-server", "1500",
        "Interval to check for the heart beat between management server nodes", false);
    final ConfigKey<Integer> HeartbeatThreshold = new ConfigKey<Integer>(Integer.class, "cluster.heartbeat.threshold", "management-server", "150000",
        "Threshold before self-fence the management server", true);

    /**
     * Adds a new packet to the incoming queue.
     * @param pdu protocol data unit
     */
    void OnReceiveClusterServicePdu(ClusterServicePdu pdu);

    void publishStatus(String status);

    /**
     * Creates and registers a PDU, notifies listeners, and waits on the PDU to be notified.
     * @param strPeer destination
     * @param agentId reference to a resource
     * @param cmds any json string (probably containing encoded commands)
     * @param stopOnError should the other side continue id an error is encountered
     * @return json encoded answer from the far side
     */
    String execute(String strPeer, long agentId, String cmds, boolean stopOnError);

    /**
     * Broadcast the command to all the management server nodes.
     * @param agentId agent id this broadcast is regarding
     * @param cmds commands to broadcast
     */
    void broadcast(long agentId, String cmds);

    void registerListener(ClusterManagerListener listener);

    void unregisterListener(ClusterManagerListener listener);

    void registerDispatcher(Dispatcher dispatcher);

    /**
     * Registers a listener for incoming status changes of ManagementServers.
     *
     * @param administrator the object administrating statuses
     */
    void registerStatusAdministrator(StatusAdministrator administrator);

    ManagementServerHost getPeer(String peerName);

    /**
     *
     * @return A {code}Long.toString({code}{@see getManagementNodeId()}{code}){code} representation of the PID of the management server process.
     */
    String getSelfPeerName();

    String getSelfNodeIP();

    long getManagementNodeId();

    /**
     * determined by the time
     * @return The start time of the management server as {code}System.currentTimeMillis(){code}.
     */
    long getCurrentRunId();

    /**
     * @return The other MS's id as derived from start time as stored in the db.
     */
    long getManagementRunId(long msId);

    interface Dispatcher {
        String getName();

        String dispatch(ClusterServicePdu pdu);
    }

    /**
     * The definition of what the client of {@see registerStatusAdministrator()} should implement.
     */
    interface StatusAdministrator {
        String newStatus(ClusterServicePdu pdu);
    }
}
