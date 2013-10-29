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
package com.cloud.storage;

import java.net.UnknownHostException;

import org.apache.cloudstack.api.command.admin.storage.AddImageStoreCmd;
import org.apache.cloudstack.api.command.admin.storage.CancelPrimaryStorageMaintenanceCmd;
import org.apache.cloudstack.api.command.admin.storage.CreateSecondaryStagingStoreCmd;
import org.apache.cloudstack.api.command.admin.storage.CreateStoragePoolCmd;
import org.apache.cloudstack.api.command.admin.storage.DeleteImageStoreCmd;
import org.apache.cloudstack.api.command.admin.storage.DeletePoolCmd;
import org.apache.cloudstack.api.command.admin.storage.DeleteSecondaryStagingStoreCmd;
import org.apache.cloudstack.api.command.admin.storage.UpdateStoragePoolCmd;

import com.cloud.exception.DiscoveryException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.exception.ResourceUnavailableException;

public interface StorageService{
    /**
     * Create StoragePool based on uri
     *
     * @param cmd
     *            the command object that specifies the zone, cluster/pod, URI, details, etc. to use to create the
     *            storage pool.
     * @return
     * @throws ResourceInUseException
     * @throws IllegalArgumentException
     * @throws UnknownHostException
     * @throws ResourceUnavailableException
     *             TODO
     */
    StoragePool createPool(CreateStoragePoolCmd cmd) throws ResourceInUseException, IllegalArgumentException,
    UnknownHostException, ResourceUnavailableException;

    ImageStore createSecondaryStagingStore(CreateSecondaryStagingStoreCmd cmd);

    /**
     * Delete the storage pool
     *
     * @param cmd
     *            - the command specifying poolId
     * @return success or failure
     */
    boolean deletePool(DeletePoolCmd cmd);

    /**
     * Enable maintenance for primary storage
     *
     * @param cmd
     *            - the command specifying primaryStorageId
     * @return the primary storage pool
     * @throws ResourceUnavailableException
     *             TODO
     * @throws InsufficientCapacityException
     *             TODO
     */
    public StoragePool preparePrimaryStorageForMaintenance(Long primaryStorageId) throws ResourceUnavailableException,
    InsufficientCapacityException;

    /**
     * Complete maintenance for primary storage
     *
     * @param cmd
     *            - the command specifying primaryStorageId
     * @return the primary storage pool
     * @throws ResourceUnavailableException
     *             TODO
     */
    public StoragePool cancelPrimaryStorageForMaintenance(CancelPrimaryStorageMaintenanceCmd cmd)
            throws ResourceUnavailableException;

    public StoragePool updateStoragePool(UpdateStoragePoolCmd cmd) throws IllegalArgumentException;

    public StoragePool getStoragePool(long id);

    boolean deleteImageStore(DeleteImageStoreCmd cmd);

    boolean deleteSecondaryStagingStore(DeleteSecondaryStagingStoreCmd cmd);

    ImageStore discoverImageStore(AddImageStoreCmd cmd) throws IllegalArgumentException, DiscoveryException, InvalidParameterValueException;

    /**
     * Prepare NFS secondary storage for object store migration
     *
     * @param cmd
     *            - the command specifying secondaryStorageId
     * @return the storage pool
     * @throws ResourceUnavailableException
     *             TODO
     * @throws InsufficientCapacityException
     *             TODO
     */
    public ImageStore prepareSecondaryStorageForObjectStoreMigration(Long storeId) throws ResourceUnavailableException,
            InsufficientCapacityException;

}
