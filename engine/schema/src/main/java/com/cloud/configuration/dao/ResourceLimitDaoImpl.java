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

import java.util.ArrayList;
import java.util.List;


import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import com.cloud.configuration.Resource;
import com.cloud.configuration.Resource.ResourceOwnerType;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.ResourceCount;
import com.cloud.configuration.ResourceLimitVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class ResourceLimitDaoImpl extends GenericDaoBase<ResourceLimitVO, Long> implements ResourceLimitDao {
    private SearchBuilder<ResourceLimitVO> IdTypeTagSearch;
    private SearchBuilder<ResourceLimitVO> IdTypeNullTagSearch;
    private SearchBuilder<ResourceLimitVO> NonMatchingTagsSearch;

    public ResourceLimitDaoImpl() {
        IdTypeTagSearch = createSearchBuilder();
        IdTypeTagSearch.and("type", IdTypeTagSearch.entity().getType(), SearchCriteria.Op.EQ);
        IdTypeTagSearch.and("domainId", IdTypeTagSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        IdTypeTagSearch.and("accountId", IdTypeTagSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        IdTypeTagSearch.and("tag", IdTypeTagSearch.entity().getTag(), SearchCriteria.Op.EQ);

        IdTypeNullTagSearch = createSearchBuilder();
        IdTypeNullTagSearch.and("type", IdTypeNullTagSearch.entity().getType(), SearchCriteria.Op.EQ);
        IdTypeNullTagSearch.and("domainId", IdTypeNullTagSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        IdTypeNullTagSearch.and("accountId", IdTypeNullTagSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        IdTypeNullTagSearch.and("tag", IdTypeNullTagSearch.entity().getTag(), SearchCriteria.Op.NULL);
        IdTypeNullTagSearch.done();

        NonMatchingTagsSearch = createSearchBuilder();
        NonMatchingTagsSearch.and("accountId", NonMatchingTagsSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        NonMatchingTagsSearch.and("domainId", NonMatchingTagsSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        NonMatchingTagsSearch.and("types", NonMatchingTagsSearch.entity().getType(), SearchCriteria.Op.IN);
        NonMatchingTagsSearch.and("tagNotNull", NonMatchingTagsSearch.entity().getTag(), SearchCriteria.Op.NNULL);
        NonMatchingTagsSearch.and("tags", NonMatchingTagsSearch.entity().getTag(), SearchCriteria.Op.NIN);
        NonMatchingTagsSearch.done();
    }

    @Override
    public List<ResourceLimitVO> listByOwner(Long ownerId, ResourceOwnerType ownerType) {
        SearchCriteria<ResourceLimitVO> sc = IdTypeTagSearch.create();

        if (ownerType == ResourceOwnerType.Account) {
            sc.setParameters("accountId", ownerId);
            return listBy(sc);
        } else if (ownerType == ResourceOwnerType.Domain) {
            sc.setParameters("domainId", ownerId);
            return listBy(sc);
        } else {
            return new ArrayList<ResourceLimitVO>();
        }
    }

    @Override
    public boolean update(Long id, Long max) {
        ResourceLimitVO limit = findById(id);
        if (max != null)
            limit.setMax(max);
        else
            limit.setMax(new Long(-1));
        return update(id, limit);
    }

    @Override
    public ResourceCount.ResourceType getLimitType(String type) {
        ResourceType[] validTypes = Resource.ResourceType.values();

        for (ResourceType validType : validTypes) {
            if (validType.getName().equals(type)) {
                return validType;
            }
        }
        return null;
    }

    @Override
    public ResourceLimitVO findByOwnerIdAndTypeAndTag(long ownerId, ResourceOwnerType ownerType, ResourceCount.ResourceType type, String tag) {
        SearchCriteria<ResourceLimitVO> sc = tag != null ? IdTypeTagSearch.create() : IdTypeNullTagSearch.create();
        sc.setParameters("type", type);
        if (tag != null) {
            sc.setParameters("tag", tag);
        }

        if (ownerType == ResourceOwnerType.Account) {
            sc.setParameters("accountId", ownerId);
            return findOneBy(sc);
        } else if (ownerType == ResourceOwnerType.Domain) {
            sc.setParameters("domainId", ownerId);
            return findOneBy(sc);
        } else {
            return null;
        }
    }

    @Override
    public long removeEntriesByOwner(Long ownerId, ResourceOwnerType ownerType) {
        SearchCriteria<ResourceLimitVO> sc = IdTypeTagSearch.create();

        if (ownerType == ResourceOwnerType.Account) {
            sc.setParameters("accountId", ownerId);
            return remove(sc);
        } else if (ownerType == ResourceOwnerType.Domain) {
            sc.setParameters("domainId", ownerId);
            return remove(sc);
        }
        return 0;
    }

    @Override
    public void removeResourceLimitsForNonMatchingTags(Long ownerId, ResourceOwnerType ownerType, List<ResourceType> types, List<String> tags) {
        SearchCriteria<ResourceLimitVO> sc = NonMatchingTagsSearch.create();
        if (ObjectUtils.allNotNull(ownerId, ownerType)) {
            if (ResourceOwnerType.Account.equals(ownerType)) {
                sc.setParameters("accountId", ownerId);
            } else {
                sc.setParameters("domainId", ownerId);
            }
        }
        if (CollectionUtils.isNotEmpty(types)) {
            sc.setParameters("types", types.stream().map(ResourceType::getName).toArray());
        }
        if (CollectionUtils.isNotEmpty(tags)) {
            sc.setParameters("tags", tags.toArray());
        }
        remove(sc);
    }
}
