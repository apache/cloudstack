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
import org.apache.log4j.Logger;

public class ResourceIconDaoImpl extends GenericDaoBase<ResourceIconVO, Long> implements ResourceIconDao {
    public static final Logger s_logger = Logger.getLogger(ResourceIconDaoImpl.class);
    private final SearchBuilder<ResourceIconVO> AllFieldsSearch;

    protected ResourceIconDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("resourceId", AllFieldsSearch.entity().getResourceId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("uuid", AllFieldsSearch.entity().getResourceUuid(), SearchCriteria.Op.EQ);
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
        sc.setParameters("uuid", resourceUuid);
        sc.setParameters("resourceType", resourceType);
        return findOneBy(sc);
    }
}
