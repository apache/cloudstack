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
import java.util.Map;

import org.apache.cloudstack.api.command.admin.storage.CancelPrimaryStorageMaintenanceCmd;
import org.apache.cloudstack.api.command.admin.storage.CreateSecondaryStagingStoreCmd;
import org.apache.cloudstack.api.command.admin.storage.CreateStoragePoolCmd;
import org.apache.cloudstack.api.command.admin.storage.DeleteImageStoreCmd;
import org.apache.cloudstack.api.command.admin.storage.DeletePoolCmd;
import org.apache.cloudstack.api.command.admin.storage.DeleteSecondaryStagingStoreCmd;
import org.apache.cloudstack.api.command.admin.storage.UpdateStoragePoolCmd;
import org.apache.cloudstack.api.command.admin.storage.SyncStoragePoolCmd;

import com.cloud.exception.DiscoveryException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.exception.ResourceUnavailableException;

public interface StorageService {
    /**
     * Create StoragePool based on uri
     *
     * @param cmd
     *            The command object that specifies the zone, cluster/pod, URI, details, etc. to use to create the
     *            storage pool.
     * @return
     *            The StoragePool created.
     * @throws ResourceInUseException
     * @throws IllegalArgumentException
     * @throws UnknownHostException
     * @throws ResourceUnavailableException
     */
    StoragePool createPool(CreateStoragePoolCmd cmd) throws ResourceInUseException, IllegalArgumentException, UnknownHostException, ResourceUnavailableException;

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
     * @param primaryStorageId
     *            - the primaryStorageId
     * @return the primary storage pool
     * @throws ResourceUnavailableException
     * @throws InsufficientCapacityException
     */
    StoragePool preparePrimaryStorageForMaintenance(Long primaryStorageId) throws ResourceUnavailableException, InsufficientCapacityException;

    /**
     * Complete maintenance for primary storage
     *
     * @param cmd
     *            - the command specifying primaryStorageId
     * @return the primary storage pool
     * @throws ResourceUnavailableException
     */
    StoragePool cancelPrimaryStorageForMaintenance(CancelPrimaryStorageMaintenanceCmd cmd) throws ResourceUnavailableException;

    StoragePool updateStoragePool(UpdateStoragePoolCmd cmd) throws IllegalArgumentException;

    StoragePool getStoragePool(long id);

    boolean deleteImageStore(DeleteImageStoreCmd cmd);

    boolean deleteSecondaryStagingStore(DeleteSecondaryStagingStoreCmd cmd);

    ImageStore discoverImageStore(String name, String url, String providerName, Long zoneId, Map details) throws IllegalArgumentException, DiscoveryException, InvalidParameterValueException;

    /**
     * Migrate existing NFS to use object store.
     * @param name object store name.
     * @param url object store URL.
     * @param providerName object store provider Name.
     * @param details object store other details
     * @return Object store created.
     */
    ImageStore migrateToObjectStore(String name, String url, String providerName, Map<String, String> details) throws DiscoveryException;

    ImageStore updateImageStoreStatus(Long id, Boolean readonly);

    StoragePool syncStoragePool(SyncStoragePoolCmd cmd);

}
