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

import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.server.ResourceTag;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.tags.ResourceTagVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
@Local(value = { ResourceTagDao.class })
public class ResourceTagsDaoImpl extends GenericDaoBase<ResourceTagVO, Long> implements ResourceTagDao{
    final SearchBuilder<ResourceTagVO> AllFieldsSearch;
    
    public ResourceTagsDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("resourceId", AllFieldsSearch.entity().getResourceId(), Op.EQ);
        AllFieldsSearch.and("uuid", AllFieldsSearch.entity().getResourceUuid(), Op.EQ);
        AllFieldsSearch.and("resourceType", AllFieldsSearch.entity().getResourceType(), Op.EQ);
        AllFieldsSearch.done();
    }
    
    @Override
    public boolean removeByIdAndType(long resourceId, ResourceTag.TaggedResourceType resourceType) {
        SearchCriteria<ResourceTagVO> sc = AllFieldsSearch.create();
        sc.setParameters("resourceId", resourceId);
        sc.setParameters("resourceType", resourceType);
        remove(sc);
        return true;
    }

    @Override
    public List<? extends ResourceTag> listBy(long resourceId, TaggedResourceType resourceType) {
        SearchCriteria<ResourceTagVO> sc = AllFieldsSearch.create();
        sc.setParameters("resourceId", resourceId);
        sc.setParameters("resourceType", resourceType);
        return listBy(sc);
    }
}
