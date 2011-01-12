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
package com.cloud.storage;

import java.net.UnknownHostException;

import com.cloud.api.ServerApiException;
import com.cloud.api.commands.CancelPrimaryStorageMaintenanceCmd;
import com.cloud.api.commands.CreateStoragePoolCmd;
import com.cloud.api.commands.CreateVolumeCmd;
import com.cloud.api.commands.DeletePoolCmd;
import com.cloud.api.commands.DeleteVolumeCmd;
import com.cloud.api.commands.PreparePrimaryStorageForMaintenanceCmd;
import com.cloud.api.commands.UpdateStoragePoolCmd;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceInUseException;

public interface StorageService {
    /**
     * Create StoragePool based on uri
     * @param cmd the command object that specifies the zone, cluster/pod, URI, details, etc. to use to create the storage pool.
     * @return
     * @throws ResourceInUseException
     * @throws IllegalArgumentException
     * @throws UnknownHostException
     * @throws ResourceAllocationException
     */
    StoragePool createPool(CreateStoragePoolCmd cmd) throws ResourceInUseException, IllegalArgumentException, UnknownHostException, ResourceAllocationException;
    
    /**
     * Creates the database object for a volume based on the given criteria
     * @param cmd the API command wrapping the criteria (account/domainId [admin only], zone, diskOffering, snapshot, name)
     * @return the volume object
     * @throws InvalidParameterValueException
     * @throws PermissionDeniedException
     */
    Volume allocVolume(CreateVolumeCmd cmd) throws ResourceAllocationException;

    /**
     * Creates the volume based on the given criteria
     * @param cmd the API command wrapping the criteria (account/domainId [admin only], zone, diskOffering, snapshot, name)
     * @return the volume object
     */
    Volume createVolume(CreateVolumeCmd cmd);

    boolean deleteVolume(DeleteVolumeCmd cmd) throws ConcurrentOperationException;
    /**
     * Delete the storage pool
     * @param cmd - the command specifying poolId
     * @return success or failure
     * @throws InvalidParameterValueException
     */
    boolean deletePool(DeletePoolCmd cmd) throws InvalidParameterValueException;
    /**
     * Enable maintenance for primary storage
     * @param cmd - the command specifying primaryStorageId
     * @return the primary storage pool
     * @throws ServerApiException
     */
    public StoragePool preparePrimaryStorageForMaintenance(PreparePrimaryStorageForMaintenanceCmd cmd) throws ServerApiException;
    
    /**
     * Complete maintenance for primary storage
     * @param cmd - the command specifying primaryStorageId
     * @return the primary storage pool
     * @throws ServerApiException
     */
    public StoragePool cancelPrimaryStorageForMaintenance(CancelPrimaryStorageMaintenanceCmd cmd) throws ServerApiException;

    public StoragePool updateStoragePool(UpdateStoragePoolCmd cmd) throws IllegalArgumentException;
    



}
