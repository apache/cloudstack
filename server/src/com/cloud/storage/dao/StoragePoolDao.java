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
package com.cloud.storage.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.StoragePoolVO;
import com.cloud.utils.db.GenericDao;
/**
 * Data Access Object for storage_pool table
 */
public interface StoragePoolDao extends GenericDao<StoragePoolVO, Long> {

	/**
	 * @param datacenterId -- the id of the datacenter (availability zone)
	 * @return the list of storage pools in the datacenter
	 */
	List<StoragePoolVO> listByDataCenterId(long datacenterId);
	
	/**
	 * @param datacenterId -- the id of the datacenter (availability zone)
	 * @param podId the id of the pod
	 * @return the list of storage pools in the datacenter
	 */
	List<StoragePoolVO> listBy(long datacenterId, long podId, Long clusterId);
    
	/**
	 * Set capacity of storage pool in bytes
	 * @param id pool id.
	 * @param capacity capacity in bytes
	 */
    void updateCapacity(long id, long capacity);
    
	/**
	 * Set available bytes of storage pool in bytes
	 * @param id pool id.
	 * @param available available capacity in bytes
	 */
    void updateAvailable(long id, long available);
        
    
    StoragePoolVO persist(StoragePoolVO pool, Map<String, String> details);
    
    /**
     * Find pool by name.
     * 
     * @param name name of pool.
     * @return the single  StoragePoolVO
     */
    List<StoragePoolVO> findPoolByName(String name);
    
    /**
     * Find pools by the pod that matches the details.
     * 
     * @param podId pod id to find the pools in.
     * @param details details to match.  All must match for the pool to be returned.
     * @return List of StoragePoolVO
     */
    List<StoragePoolVO> findPoolsByDetails(long dcId, long podId, Long clusterId, Map<String, String> details);
    
    List<StoragePoolVO> findPoolsByTags(long dcId, long podId, Long clusterId, String[] tags, Boolean shared);
    
    /**
     * Find pool by UUID.
     * 
     * @param uuid uuid of pool.
     * @return the single  StoragePoolVO
     */
    StoragePoolVO findPoolByUUID(String uuid);

    List<StoragePoolVO> listByStorageHost(String hostFqdnOrIp);

    StoragePoolVO findPoolByHostPath(long dcId, Long podId, String host, String path, String uuid);
    
    List<StoragePoolVO> listPoolByHostPath(String host, String path);
    
    void deleteStoragePoolRecords(ArrayList<Long> ids);
    
    void updateDetails(long poolId, Map<String, String> details);
    
    Map<String, String> getDetails(long poolId);

	List<String> searchForStoragePoolDetails(long poolId, String value);
	
	List<StoragePoolVO> findIfDuplicatePoolsExistByUUID(String uuid);

    List<StoragePoolVO> listByStatus(StoragePoolStatus status);

    long countPoolsByStatus(StoragePoolStatus... statuses);

	List<StoragePoolVO> listByStatusInZone(long dcId, StoragePoolStatus status);
    
    List<StoragePoolVO> listPoolsByCluster(long clusterId);
}
