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
package org.apache.cloudstack.storage.datastore.db;

import java.util.List;
import java.util.Map;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.ScopeType;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.utils.db.GenericDao;

/**
 * Data Access Object for storage_pool table
 */
public interface PrimaryDataStoreDao extends GenericDao<StoragePoolVO, Long> {

    /**
     * @param datacenterId -- the id of the datacenter (availability zone)
     */
    List<StoragePoolVO> listByDataCenterId(long datacenterId);

    /**
     * @param datacenterId -- the id of the datacenter (availability zone)
     */
    List<StoragePoolVO> listBy(long datacenterId, Long podId, Long clusterId, ScopeType scope);

    /**
     * Set capacity of storage pool in bytes
     * @param id pool id.
     * @param capacityBytes capacity in bytes
     */
    void updateCapacityBytes(long id, long capacityBytes);

    /**
     * Set iops capacity of storage pool
     * @param id pool id.
     * @param capacityIops iops capacity
     */
    void updateCapacityIops(long id, long capacityIops);

    StoragePoolVO persist(StoragePoolVO pool, Map<String, String> details);

    /**
     * Find pool by name.
     *
     * @param name
     *            name of pool.
     * @return the single StoragePoolVO
     */
    List<StoragePoolVO> findPoolByName(String name);

    /**
     * Find pools by the pod that matches the details.
     *
     * @param podId
     *            pod id to find the pools in.
     * @param details
     *            details to match. All must match for the pool to be returned.
     * @return List of StoragePoolVO
     */
    List<StoragePoolVO> findPoolsByDetails(long dcId, long podId, Long clusterId, Map<String, String> details, ScopeType scope);

    List<StoragePoolVO> findPoolsByTags(long dcId, long podId, Long clusterId, String[] tags);

    List<StoragePoolVO> findDisabledPoolsByScope(long dcId, Long podId, Long clusterId, ScopeType scope);

    /**
     * Find pool by UUID.
     *
     * @param uuid
     *            uuid of pool.
     * @return the single StoragePoolVO
     */
    StoragePoolVO findPoolByUUID(String uuid);

    List<StoragePoolVO> listByStorageHost(String hostFqdnOrIp);

    StoragePoolVO findPoolByHostPath(long dcId, Long podId, String host, String path, String uuid);

    List<StoragePoolVO> listPoolByHostPath(String host, String path);

    void updateDetails(long poolId, Map<String, String> details);

    Map<String, String> getDetails(long poolId);

    List<String> searchForStoragePoolDetails(long poolId, String value);

    List<StoragePoolVO> findIfDuplicatePoolsExistByUUID(String uuid);

    List<StoragePoolVO> listByStatus(StoragePoolStatus status);

    long countPoolsByStatus(StoragePoolStatus... statuses);

    List<StoragePoolVO> listByStatusInZone(long dcId, StoragePoolStatus status);

    List<StoragePoolVO> listPoolsByCluster(long clusterId);

    List<StoragePoolVO> findLocalStoragePoolsByTags(long dcId, long podId, Long clusterId, String[] tags);

    List<StoragePoolVO> findZoneWideStoragePoolsByTags(long dcId, String[] tags);

    List<StoragePoolVO> findZoneWideStoragePoolsByHypervisor(long dataCenterId, HypervisorType hypervisorType);

    List<StoragePoolVO> findLocalStoragePoolsByHostAndTags(long hostId, String[] tags);

    List<StoragePoolVO> listLocalStoragePoolByPath(long datacenterId, String path);
}
