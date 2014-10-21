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
     * @param domainId the id of the domain to get the resource count
     * @param type the type of resource (e.g. user_vm, public_ip, volume)
     * @return the count of resources in use for the given type and domain
     */
    long getResourceCount(long ownerId, ResourceOwnerType ownerType, ResourceType type);

    /**
     * @param domainId the id of the domain to set the resource count
     * @param type the type of resource (e.g. user_vm, public_ip, volume)
     * @param the count of resources in use for the given type and domain
     */
    void setResourceCount(long ownerId, ResourceOwnerType ownerType, ResourceType type, long count);

    @Deprecated
    void updateDomainCount(long domainId, ResourceType type, boolean increment, long delta);

    boolean updateById(long id, boolean increment, long delta);

    void createResourceCounts(long ownerId, ResourceOwnerType ownerType);

    List<ResourceCountVO> listByOwnerId(long ownerId, ResourceOwnerType ownerType);

    ResourceCountVO findByOwnerAndType(long ownerId, ResourceOwnerType ownerType, ResourceType type);

    List<ResourceCountVO> listResourceCountByOwnerType(ResourceOwnerType ownerType);

    Set<Long> listAllRowsToUpdate(long ownerId, ResourceOwnerType ownerType, ResourceType type);

    Set<Long> listRowsToUpdateForDomain(long domainId, ResourceType type);

    long removeEntriesByOwner(long ownerId, ResourceOwnerType ownerType);
}
