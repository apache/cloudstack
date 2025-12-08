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
package com.cloud.resource;

import java.util.List;

import org.apache.cloudstack.api.command.admin.cluster.AddClusterCmd;
import org.apache.cloudstack.api.command.admin.cluster.DeleteClusterCmd;
import org.apache.cloudstack.api.command.admin.cluster.UpdateClusterCmd;
import org.apache.cloudstack.api.command.admin.host.AddHostCmd;
import org.apache.cloudstack.api.command.admin.host.AddSecondaryStorageCmd;
import org.apache.cloudstack.api.command.admin.host.CancelMaintenanceCmd;
import org.apache.cloudstack.api.command.admin.host.ReconnectHostCmd;
import org.apache.cloudstack.api.command.admin.host.UpdateHostCmd;
import org.apache.cloudstack.api.command.admin.host.UpdateHostPasswordCmd;
import org.apache.cloudstack.api.command.admin.host.PrepareForMaintenanceCmd;
import org.apache.cloudstack.api.command.admin.host.DeclareHostAsDegradedCmd;
import org.apache.cloudstack.api.command.admin.host.CancelHostAsDegradedCmd;

import com.cloud.dc.DataCenter;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.org.Cluster;
import com.cloud.utils.fsm.NoTransitionException;

public interface ResourceService {
    /**
     * Updates a host
     *
     * @param cmd - the command specifying hostId
     */
    Host updateHost(UpdateHostCmd cmd) throws NoTransitionException;

    Host autoUpdateHostAllocationState(Long hostId, ResourceState.Event resourceEvent) throws NoTransitionException;

    Host cancelMaintenance(CancelMaintenanceCmd cmd);

    Host reconnectHost(ReconnectHostCmd cmd) throws AgentUnavailableException;

    /**
     * We will automatically create an Apache CloudStack cluster to attach to the external cluster and return a hyper host to perform
     * host related operation within the cluster
     */
    List<? extends Cluster> discoverCluster(AddClusterCmd cmd) throws IllegalArgumentException, DiscoveryException, ResourceInUseException;

    boolean deleteCluster(DeleteClusterCmd cmd);

    Cluster updateCluster(UpdateClusterCmd cmd);

    List<? extends Host> discoverHosts(AddHostCmd cmd) throws IllegalArgumentException, DiscoveryException, InvalidParameterValueException;

    List<? extends Host> discoverHosts(AddSecondaryStorageCmd cmd) throws IllegalArgumentException, DiscoveryException, InvalidParameterValueException;

    Host maintain(PrepareForMaintenanceCmd cmd);

    Host declareHostAsDegraded(DeclareHostAsDegradedCmd cmd) throws NoTransitionException;

    Host cancelHostAsDegraded(CancelHostAsDegradedCmd cmd) throws NoTransitionException;

    /**
     * Deletes a host
     * @param true if deleted, false otherwise
     */
    boolean deleteHost(long hostId, boolean isForced, boolean isForceDeleteStorage);

    boolean updateClusterPassword(UpdateHostPasswordCmd upasscmd);

    boolean updateHostPassword(UpdateHostPasswordCmd upasscmd);

    Host getHost(long hostId);

    Cluster getCluster(Long clusterId);

    DataCenter getZone(Long zoneId);

    List<HypervisorType> getSupportedHypervisorTypes(long zoneId, boolean forVirtualRouter, Long podId);

    boolean releaseHostReservation(Long hostId);

}
