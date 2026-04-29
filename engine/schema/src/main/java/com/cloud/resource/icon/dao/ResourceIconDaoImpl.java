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
package com.cloud.resource.icon.dao;

import com.cloud.resource.icon.ResourceIconVO;
import com.cloud.server.ResourceIcon;
import com.cloud.server.ResourceTag;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.response.ResourceIconResponse;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ResourceIconDaoImpl extends GenericDaoBase<ResourceIconVO, Long> implements ResourceIconDao {
    private final SearchBuilder<ResourceIconVO> AllFieldsSearch;

    protected ResourceIconDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("resourceId", AllFieldsSearch.entity().getResourceId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("uuid", AllFieldsSearch.entity().getResourceUuid(), SearchCriteria.Op.IN);
        AllFieldsSearch.and("resourceType", AllFieldsSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
    }

    @Override
    public ResourceIconResponse newResourceIconResponse(ResourceIcon resourceIcon) {
        ResourceIconResponse resourceIconResponse = new ResourceIconResponse();
        resourceIconResponse.setResourceId(resourceIcon.getResourceUuid());
        resourceIconResponse.setResourceType(resourceIcon.getResourceType());
        resourceIconResponse.setImage(resourceIcon.getIcon());
        resourceIconResponse.setObjectName(ApiConstants.RESOURCE_ICON);
        return resourceIconResponse;
    }

    @Override
    public ResourceIconVO findByResourceUuid(String resourceUuid, ResourceTag.ResourceObjectType resourceType) {
        SearchCriteria<ResourceIconVO> sc = AllFieldsSearch.create();
        sc.setParameters("uuid", (Object[]) new String[]{resourceUuid});
        sc.setParameters("resourceType", resourceType);
        return findOneBy(sc);
    }

    @Override
    public List<ResourceIconVO> listByResourceTypeAndIds(ResourceTag.ResourceObjectType resourceType,
                Collection<Long> resourceIds) {
        if (CollectionUtils.isEmpty(resourceIds)) {
            return new ArrayList<>();
        }
        SearchBuilder<ResourceIconVO> sb = createSearchBuilder();
        sb.and("resourceId", sb.entity().getResourceId(), SearchCriteria.Op.IN);
        sb.and("resourceType", sb.entity().getResourceType(), SearchCriteria.Op.EQ);
        sb.done();
        SearchCriteria<ResourceIconVO> sc = sb.create();
        sc.setParameters("resourceId", resourceIds.toArray());
        sc.setParameters("resourceType", resourceType);
        return listBy(sc);
    }

    @Override
    public List<ResourceIconVO> listByResourceTypeAndUuids(ResourceTag.ResourceObjectType resourceType,
                Collection<String> resourceUuids) {
        if (CollectionUtils.isEmpty(resourceUuids)) {
            return new ArrayList<>();
        }
        SearchCriteria<ResourceIconVO> sc = AllFieldsSearch.create();
        sc.setParameters("uuid", resourceUuids.toArray());
        sc.setParameters("resourceType", resourceType);
        return listBy(sc);
    }

    @Override
    public List<ResourceIconResponse> listResourceIcons(List<String> resourceUuids, ResourceTag.ResourceObjectType resourceType) {
        List<ResourceIconVO> resourceIcons = listByResourceTypeAndUuids(resourceType, resourceUuids);
        List<ResourceIconResponse> iconResponses = new ArrayList<>();
        for (ResourceIconVO resourceIcon : resourceIcons) {
            ResourceIconResponse response = new ResourceIconResponse();
            response.setResourceId(resourceIcon.getResourceUuid());
            response.setResourceType(resourceIcon.getResourceType());
            response.setImage(resourceIcon.getIcon());
            response.setObjectName(ApiConstants.RESOURCE_ICON);
            iconResponses.add(response);
        }
        return iconResponses;
    }
}
