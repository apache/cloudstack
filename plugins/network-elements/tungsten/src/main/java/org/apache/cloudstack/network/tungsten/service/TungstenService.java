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
package org.apache.cloudstack.network.tungsten.service;

import com.cloud.dc.HostPodVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.network.Network;
import com.cloud.network.lb.LoadBalancingRule;

public interface TungstenService {
    String getProject(long accountId);

    String getTungstenProjectFqn(Network network);

    boolean createPublicNetwork(long zoneId);

    boolean addPublicNetworkSubnet(Vlan vlan);

    boolean removePublicNetworkSubnet(VlanVO vlanVO);

    boolean deletePublicNetwork(long zoneId);

    boolean createManagementNetwork(long zoneId);

    boolean deleteManagementNetwork(long zoneId);

    boolean addManagementNetworkSubnet(HostPodVO pod);

    boolean removeManagementNetworkSubnet(HostPodVO pod);

    boolean updateLoadBalancer(Network network, LoadBalancingRule rule);

    boolean synchronizeTungstenData(Long tungstenProviderId);

    void subscribeTungstenEvent();

    String MESSAGE_APPLY_NETWORK_POLICY_EVENT = "Message.ApplyNetworkPolicy.Event";
    String MESSAGE_CREATE_TUNGSTEN_NETWORK_EVENT = "Message.CreateTungstenNetwork.Event";
    String MESSAGE_CREATE_TUNGSTEN_LOGICAL_ROUTER_EVENT = "Message.CreateTungstenLogicalRouter.Event";
    String MESSAGE_SYNC_TUNGSTEN_DB_WITH_DOMAINS_AND_PROJECTS_EVENT = "Message.SyncTungstenDnWithDomainsAndProjects"
        + ".Event";
}
