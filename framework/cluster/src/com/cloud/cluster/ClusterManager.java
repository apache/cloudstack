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

import com.cloud.utils.component.Manager;

public interface ClusterManager extends Manager {
    static final String ALERT_SUBJECT = "cluster-alert";
    final ConfigKey<Integer> HeartbeatInterval = new ConfigKey<Integer>(Integer.class, "cluster.heartbeat.interval", "management-server", "1500",
        "Interval to check for the heart beat between management server nodes", false);
    final ConfigKey<Integer> HeartbeatThreshold = new ConfigKey<Integer>(Integer.class, "cluster.heartbeat.threshold", "management-server", "150000",
        "Threshold before self-fence the management server", true);
    final ConfigKey<String> ManagementHostIPAdr = new ConfigKey<String>("Advanced", String.class, "host", "localhost", "The ip address of management server", true);

    void OnReceiveClusterServicePdu(ClusterServicePdu pdu);

    /**
     * This executes
     * @param strPeer
     * @param agentId
     * @param cmds
     * @param stopOnError
     * @return
     */
    String execute(String strPeer, long agentId, String cmds, boolean stopOnError);

    /**
     * Broadcast the command to all of the  management server nodes.
     * @param agentId agent id this broadcast is regarding
     * @param cmds commands to broadcast
     */
    void broadcast(long agentId, String cmds);

    void registerListener(ClusterManagerListener listener);

    void unregisterListener(ClusterManagerListener listener);

    void registerDispatcher(Dispatcher dispatcher);

    ManagementServerHost getPeer(String peerName);

    String getSelfPeerName();

    long getManagementNodeId();

    long getCurrentRunId();

    public interface Dispatcher {
        String getName();

        String dispatch(ClusterServicePdu pdu);
    }
}
