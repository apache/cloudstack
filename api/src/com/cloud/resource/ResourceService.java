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
package com.cloud.resource;

import java.util.List;

import com.cloud.api.commands.AddClusterCmd;
import com.cloud.api.commands.AddHostCmd;
import com.cloud.api.commands.AddSecondaryStorageCmd;
import com.cloud.api.commands.AddSwiftCmd;
import com.cloud.api.commands.CancelMaintenanceCmd;
import com.cloud.api.commands.DeleteClusterCmd;
import com.cloud.api.commands.PrepareForMaintenanceCmd;
import com.cloud.api.commands.ReconnectHostCmd;
import com.cloud.api.commands.UpdateHostCmd;
import com.cloud.api.commands.UpdateHostPasswordCmd;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.org.Cluster;
import com.cloud.storage.Swift;

public interface ResourceService {
    /**
     * Updates a host
     * 
     * @param cmd
     *            - the command specifying hostId
     * @return hostObject
     */
    Host updateHost(UpdateHostCmd cmd);

    Host cancelMaintenance(CancelMaintenanceCmd cmd);

    Host reconnectHost(ReconnectHostCmd cmd);

    /**
     * We will automatically create a cloud.com cluster to attach to the external cluster and return a hyper host to perform
     * host related operation within the cluster
     * 
     * @param cmd
     * @return
     * @throws IllegalArgumentException
     * @throws DiscoveryException
     */
    List<? extends Cluster> discoverCluster(AddClusterCmd cmd) throws IllegalArgumentException, DiscoveryException;

    boolean deleteCluster(DeleteClusterCmd cmd);

    Cluster updateCluster(Cluster cluster, String clusterType, String hypervisor, String allocationState, String managedstate);

    List<? extends Host> discoverHosts(AddHostCmd cmd) throws IllegalArgumentException, DiscoveryException, InvalidParameterValueException;

    List<? extends Host> discoverHosts(AddSecondaryStorageCmd cmd) throws IllegalArgumentException, DiscoveryException, InvalidParameterValueException;

    Host maintain(PrepareForMaintenanceCmd cmd);

    /**
     * Deletes a host
     * 
     * @param hostId
     *            TODO
     * @param isForced
     *            TODO
     * 
     * @param true if deleted, false otherwise
     */
    boolean deleteHost(long hostId, boolean isForced, boolean isForceDeleteStorage);

    boolean updateHostPassword(UpdateHostPasswordCmd upasscmd);

    Host getHost(long hostId);

    Cluster getCluster(Long clusterId);

    List<? extends Swift> discoverSwift(AddSwiftCmd addSwiftCmd) throws DiscoveryException;
    
    List<HypervisorType> getSupportedHypervisorTypes(long zoneId);
}
