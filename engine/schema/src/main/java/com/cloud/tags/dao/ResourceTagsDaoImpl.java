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
package com.cloud.tags.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cloudstack.api.response.ResourceTagResponse;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import com.cloud.server.ResourceTag;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.tags.ResourceTagVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
public class ResourceTagsDaoImpl extends GenericDaoBase<ResourceTagVO, Long> implements ResourceTagDao {
    final SearchBuilder<ResourceTagVO> AllFieldsSearch;

    public ResourceTagsDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("resourceId", AllFieldsSearch.entity().getResourceId(), Op.EQ);
        AllFieldsSearch.and("uuid", AllFieldsSearch.entity().getResourceUuid(), Op.EQ);
        AllFieldsSearch.and("resourceType", AllFieldsSearch.entity().getResourceType(), Op.EQ);
        AllFieldsSearch.and("key", AllFieldsSearch.entity().getKey(), Op.EQ);
        AllFieldsSearch.and("resourceUuid", AllFieldsSearch.entity().getResourceUuid(), Op.EQ);
        AllFieldsSearch.done();
    }

    @Override
    public boolean removeByIdAndType(long resourceId, ResourceTag.ResourceObjectType resourceType) {
        SearchCriteria<ResourceTagVO> sc = AllFieldsSearch.create();
        sc.setParameters("resourceId", resourceId);
        sc.setParameters("resourceType", resourceType);
        remove(sc);
        return true;
    }

    @Override
    public List<? extends ResourceTag> listBy(long resourceId, ResourceObjectType resourceType) {
        SearchCriteria<ResourceTagVO> sc = AllFieldsSearch.create();
        sc.setParameters("resourceId", resourceId);
        sc.setParameters("resourceType", resourceType);
        return listBy(sc);
    }

    @Override
    public ResourceTag findByKey(long resourceId, ResourceObjectType resourceType, String key) {
        SearchCriteria<ResourceTagVO> sc = AllFieldsSearch.create();
        sc.setParameters("resourceId", resourceId);
        sc.setParameters("resourceType", resourceType);
        sc.setParameters("key", key);
        return findOneBy(sc);
    }

    @Override public void updateResourceId(long srcId, long destId, ResourceObjectType resourceType) {
        SearchCriteria<ResourceTagVO> sc = AllFieldsSearch.create();
        sc.setParameters("resourceId", srcId);
        sc.setParameters("resourceType", resourceType);
        for( ResourceTagVO tag : listBy(sc)) {
            tag.setResourceId(destId);
            update(tag.getId(), tag);
        }
    }

    @Override
    public Map<String, Set<ResourceTagResponse>> listTags() {
        SearchCriteria<ResourceTagVO> sc = AllFieldsSearch.create();
        List<ResourceTagVO> resourceTagList = listBy(sc);
        Map<String, Set<ResourceTagResponse>> resourceTagMap = new HashMap();
        String resourceKey = null;
        ResourceTagResponse resourceTagResponse = null;
        for (ResourceTagVO resourceTagVO : resourceTagList) {
            resourceTagResponse = new ResourceTagResponse();
            resourceTagResponse.setKey(resourceTagVO.getKey());
            resourceTagResponse.setValue(resourceTagVO.getValue());
            Set<ResourceTagResponse> resourceTagSet = new HashSet();
            resourceKey = resourceTagVO.getResourceId() + ":" + resourceTagVO.getResourceType();
            if(resourceTagMap.get(resourceKey) != null) {
                resourceTagSet = resourceTagMap.get(resourceKey);
            }
            resourceTagSet.add(resourceTagResponse);
            resourceTagMap.put(resourceKey, resourceTagSet);
        }
        return resourceTagMap;
    }

    @Override
    public void removeByResourceIdAndKey(long resourceId, ResourceObjectType resourceType, String key) {
        SearchCriteria<ResourceTagVO> sc = AllFieldsSearch.create();
        sc.setParameters("resourceId", resourceId);
        sc.setParameters("resourceType", resourceType);
        sc.setParameters("key", key);
        remove(sc);
    }

    @Override
    public List<? extends ResourceTag> listByResourceUuid(String resourceUuid) {
        SearchCriteria<ResourceTagVO> sc = AllFieldsSearch.create();
        sc.setParameters("resourceUuid", resourceUuid);
        return listBy(sc);
    }

    @Override
    public List<String> listByResourceTypeKeyPrefixAndOwners(ResourceObjectType resourceType, String key,
                                                             List<Long> accountIds, List<Long> domainIds,
                                                             Filter filter) {
        GenericSearchBuilder<ResourceTagVO, String> sb = createSearchBuilder(String.class);
        sb.select(null, SearchCriteria.Func.DISTINCT, sb.entity().getValue());
        sb.and("resourceType", sb.entity().getResourceType(), Op.EQ);
        sb.and("key", sb.entity().getKey(), Op.LIKE);
        boolean accountIdsNotEmpty = CollectionUtils.isNotEmpty(accountIds);
        boolean domainIdsNotEmpty = CollectionUtils.isNotEmpty(domainIds);
        if (accountIdsNotEmpty || domainIdsNotEmpty) {
            sb.and().op("account", sb.entity().getAccountId(), SearchCriteria.Op.IN);
            sb.or("domain", sb.entity().getDomainId(), SearchCriteria.Op.IN);
            sb.cp();
        }
        sb.done();
        final SearchCriteria<String> sc = sb.create();
        sc.setParameters("resourceType", resourceType);
        sc.setParameters("key", key + "%");
        if (accountIdsNotEmpty) {
            sc.setParameters("account", accountIds.toArray());
        }
        if (domainIdsNotEmpty) {
            sc.setParameters("domain", domainIds.toArray());
        }
        return customSearch(sc, filter);
    }

    @Override
    public ResourceTagVO findByResourceTypeKeyPrefixAndValue(ResourceObjectType resourceType, String key,
                                                             String value) {
        SearchBuilder<ResourceTagVO> sb = createSearchBuilder();
        sb.and("resourceType", sb.entity().getResourceType(), Op.EQ);
        sb.and("key", sb.entity().getKey(), Op.LIKE);
        sb.and("value", sb.entity().getValue(), Op.EQ);
        sb.done();
        final SearchCriteria<ResourceTagVO> sc = sb.create();
        sc.setParameters("resourceType", resourceType);
        sc.setParameters("key", key + "%");
        sc.setParameters("value", value);
        return findOneBy(sc);
    }

    @Override
    public List<ResourceTagVO> listByResourceTypeIdAndKeyPrefix(ResourceObjectType resourceType, long resourceId,
                                                                String key) {
        SearchBuilder<ResourceTagVO> sb = createSearchBuilder();
        sb.and("resourceType", sb.entity().getResourceType(), Op.EQ);
        sb.and("resourceId", sb.entity().getResourceId(), Op.EQ);
        sb.and("key", sb.entity().getKey(), Op.LIKE);
        sb.done();
        final SearchCriteria<ResourceTagVO> sc = sb.create();
        sc.setParameters("resourceType", resourceType);
        sc.setParameters("resourceId", resourceId);
        sc.setParameters("key", key + "%");
        return listBy(sc);
    }
}
