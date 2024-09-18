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
package com.cloud.configuration.dao;

import java.util.List;
import java.util.Set;

import com.cloud.configuration.Resource.ResourceOwnerType;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.ResourceCountVO;
import com.cloud.utils.db.GenericDao;

public interface ResourceCountDao extends GenericDao<ResourceCountVO, Long> {
    /**
     * @param ownerId the id of the owner to get the resource count
     * @param type the type of resource (e.g. user_vm, public_ip, volume)
     * @param tag for the type of resource
     * @return the count of resources in use for the given type and domain
     */
    long getResourceCount(long ownerId, ResourceOwnerType ownerType, ResourceType type, String tag);

    /**
     * @param ownerId the id of the owner to set the resource count
     * @param type the type of resource (e.g. user_vm, public_ip, volume)
     * @param tag the tag for the type of resource
     * @param count the count of resources in use for the given type and domain
     */
    void setResourceCount(long ownerId, ResourceOwnerType ownerType, ResourceType type, String tag, long count);

    boolean updateById(long id, boolean increment, long delta);

    void createResourceCounts(long ownerId, ResourceOwnerType ownerType);

    List<ResourceCountVO> listByOwnerId(long ownerId, ResourceOwnerType ownerType);

    ResourceCountVO findByOwnerAndTypeAndTag(long ownerId, ResourceOwnerType ownerType, ResourceType type, String tag);

    List<ResourceCountVO> findByOwnersAndTypeAndTag(List<Long> ownerIdList, ResourceOwnerType ownerType,
            ResourceType type, String tag);

    List<ResourceCountVO> listResourceCountByOwnerType(ResourceOwnerType ownerType);

    Set<Long> listAllRowsToUpdate(long ownerId, ResourceOwnerType ownerType, ResourceType type, String tag);

    boolean updateCountByDeltaForIds(List<Long> ids, boolean increment, long delta);

    Set<Long> listRowsToUpdateForDomain(long domainId, ResourceType type, String tag);

    long removeEntriesByOwner(long ownerId, ResourceOwnerType ownerType);

    /**
     * Counts the number of CPU cores allocated for the given account.
     *
     * Side note: This method is not using the "resource_count" table. It is executing the actual count instead.
     */
    long countCpuNumberAllocatedToAccount(long accountId);

    /**
     * Counts the amount of memory allocated for the given account.
     *
     * Side note: This method is not using the "resource_count" table. It is executing the actual count instead.
     */
    long countMemoryAllocatedToAccount(long accountId);

    void removeResourceCountsForNonMatchingTags(Long ownerId, ResourceOwnerType ownerType, List<ResourceType> types, List<String> tags);

    List<ResourceCountVO> lockRows(Set<Long> ids);
}
