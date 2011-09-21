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

package com.cloud.configuration.dao;

import java.util.List;
import java.util.Set;

import com.cloud.configuration.Resource.ResourceOwnerType;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.ResourceCountVO;
import com.cloud.utils.db.GenericDao;

public interface ResourceCountDao extends GenericDao<ResourceCountVO, Long> {
	/**
     * Get the count of in use resources for a owner by type
     * @param domainId the id of the domain to get the resource count
     * @param type the type of resource (e.g. user_vm, public_ip, volume)
     * @return the count of resources in use for the given type and domain
     * @param ownertype the type of the owner - can be Account and Domain
     */
	long getResourceCount(long ownerId, ResourceOwnerType ownerType, ResourceType type);

    /**
     * Get the count of in use resources for a resource by type
     * @param domainId the id of the domain to set the resource count
     * @param type the type of resource (e.g. user_vm, public_ip, volume)
     * @param the count of resources in use for the given type and domain
     * @param ownertype the type of the owner - can be Account and Domain
     */
	void setResourceCount(long ownerId, ResourceOwnerType ownerType, ResourceType type, long count);
	
	//this api is deprecated as it's used by upgrade code only
	@Deprecated 
	void updateDomainCount(long domainId, ResourceType type, boolean increment, long delta);

    boolean updateById(long id, boolean increment, long delta);

    void createResourceCounts(long ownerId, ResourceOwnerType ownerType);
    
    List<ResourceCountVO> listByOwnerId(long ownerId, ResourceOwnerType ownerType);
    
    ResourceCountVO findByOwnerAndType(long ownerId, ResourceOwnerType ownerType, ResourceType type);
    
    List<ResourceCountVO> listResourceCountByOwnerType(ResourceOwnerType ownerType);
    
    Set<Long> listAllRowsToUpdate(long ownerId, ResourceOwnerType ownerType, ResourceType type);
}
