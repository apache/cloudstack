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
import com.cloud.api.commands.CancelMaintenanceCmd;
import com.cloud.api.commands.DeleteClusterCmd;
import com.cloud.api.commands.DeleteHostCmd;
import com.cloud.api.commands.PrepareForMaintenanceCmd;
import com.cloud.api.commands.ReconnectHostCmd;
import com.cloud.api.commands.UpdateHostCmd;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.org.Cluster;

public interface ResourceService {
    /**
     * Updates a host
     * @param cmd - the command specifying hostId
     * @return hostObject
     * @throws InvalidParameterValueException
     */
    Host updateHost(UpdateHostCmd cmd) throws InvalidParameterValueException;

    Host cancelMaintenance(CancelMaintenanceCmd cmd) throws InvalidParameterValueException;

    Host reconnectHost(ReconnectHostCmd cmd) throws AgentUnavailableException;
    
    /**
     * We will automatically create a cloud.com cluster to attach to the external cluster and return a hyper host to perform 
     * host related operation within the cluster
     * 
     * @param cmd
     * @return
     * @throws IllegalArgumentException
     * @throws DiscoveryException
     * @throws InvalidParameterValueException
     */
    List<? extends Cluster> discoverCluster(AddClusterCmd cmd) throws IllegalArgumentException, DiscoveryException, InvalidParameterValueException;
    boolean deleteCluster(DeleteClusterCmd cmd) throws InvalidParameterValueException; 
    
    List<? extends Host> discoverHosts(AddHostCmd cmd) throws IllegalArgumentException, DiscoveryException, InvalidParameterValueException;
    List<? extends Host> discoverHosts(AddSecondaryStorageCmd cmd) throws IllegalArgumentException, DiscoveryException, InvalidParameterValueException;
    Host maintain(PrepareForMaintenanceCmd cmd) throws InvalidParameterValueException;
    /**
     * Deletes a host
     * 
     * @param cmd - the command specifying hostId
     * @param true if deleted, false otherwise
     * @throws InvalidParameterValueException
     */
    boolean deleteHost(DeleteHostCmd cmd) throws InvalidParameterValueException; 
}
