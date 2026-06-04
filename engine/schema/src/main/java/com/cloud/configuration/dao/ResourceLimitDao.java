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

import com.cloud.configuration.Resource;
import com.cloud.configuration.Resource.ResourceOwnerType;
import com.cloud.configuration.ResourceCount;
import com.cloud.configuration.ResourceLimitVO;
import com.cloud.utils.db.GenericDao;

public interface ResourceLimitDao extends GenericDao<ResourceLimitVO, Long> {

    List<ResourceLimitVO> listByOwner(Long ownerId, ResourceOwnerType ownerType);

    boolean update(Long id, Long max);

    ResourceCount.ResourceType getLimitType(String type);

    ResourceLimitVO findByOwnerIdAndTypeAndTag(long ownerId, ResourceOwnerType ownerType, ResourceCount.ResourceType type, String tag);

    long removeEntriesByOwner(Long ownerId, ResourceOwnerType ownerType);
    void removeResourceLimitsForNonMatchingTags(Long ownerId, ResourceOwnerType ownerType, List<Resource.ResourceType> types, List<String> tags);

    /**
     * Returns the subset of {@code domainIds} that have an explicit
     * {@code resource_limit} row whose {@code max} is not
     * {@link Resource#RESOURCE_UNLIMITED} for the supplied
     * ({@code type}, {@code tag}). Domains that rely on the global default
     * are NOT returned — the caller checks
     * {@code findDefaultResourceLimitForDomain} separately.
     */
    Set<Long> listDomainIdsWithFiniteLimit(Set<Long> domainIds, Resource.ResourceType type, String tag);
}
