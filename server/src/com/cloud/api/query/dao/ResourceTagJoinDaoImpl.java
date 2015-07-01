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
package com.cloud.api.query.dao;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.api.response.ResourceTagResponse;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.api.ApiResponseHelper;
import com.cloud.api.query.vo.ResourceTagJoinVO;
import com.cloud.server.ResourceTag;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
@Local(value = {ResourceTagJoinDao.class})
public class ResourceTagJoinDaoImpl extends GenericDaoBase<ResourceTagJoinVO, Long> implements ResourceTagJoinDao {
    public static final Logger s_logger = Logger.getLogger(ResourceTagJoinDaoImpl.class);

    @Inject
    private ConfigurationDao _configDao;

    private final SearchBuilder<ResourceTagJoinVO> tagSearch;

    private final SearchBuilder<ResourceTagJoinVO> tagIdSearch;

    private final SearchBuilder<ResourceTagJoinVO> AllFieldsSearch;

    protected ResourceTagJoinDaoImpl() {

        tagSearch = createSearchBuilder();
        tagSearch.and("idIN", tagSearch.entity().getId(), SearchCriteria.Op.IN);
        tagSearch.done();

        tagIdSearch = createSearchBuilder();
        tagIdSearch.and("id", tagIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        tagIdSearch.done();

        this._count = "select count(distinct id) from resource_tag_view WHERE ";

        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("resourceId", AllFieldsSearch.entity().getResourceId(), Op.EQ);
        AllFieldsSearch.and("uuid", AllFieldsSearch.entity().getResourceUuid(), Op.EQ);
        AllFieldsSearch.and("resourceType", AllFieldsSearch.entity().getResourceType(), Op.EQ);
        AllFieldsSearch.done();
    }

    @Override
    public ResourceTagResponse newResourceTagResponse(ResourceTagJoinVO resourceTag, boolean keyValueOnly) {
        ResourceTagResponse response = new ResourceTagResponse();
        response.setKey(resourceTag.getKey());
        response.setValue(resourceTag.getValue());

        if (!keyValueOnly) {
            response.setResourceType(resourceTag.getResourceType().toString());
            response.setResourceId(resourceTag.getResourceUuid());

            ApiResponseHelper.populateOwner(response, resourceTag);

            response.setDomainId(resourceTag.getDomainUuid());
            response.setDomainName(resourceTag.getDomainName());

            response.setCustomer(resourceTag.getCustomer());
        }

        response.setObjectName("tag");

        return response;
    }

    @Override
    public List<ResourceTagJoinVO> listBy(String resourceUUID, ResourceObjectType resourceType) {
        SearchCriteria<ResourceTagJoinVO> sc = AllFieldsSearch.create();
        sc.setParameters("uuid", resourceUUID);
        sc.setParameters("resourceType", resourceType);
        return listBy(sc);
    }

    @Override
    public List<ResourceTagJoinVO> searchByIds(Long... tagIds) {
        // set detail batch query size
        int DETAILS_BATCH_SIZE = 2000;
        String batchCfg = _configDao.getValue("detail.batch.query.size");
        if (batchCfg != null) {
            DETAILS_BATCH_SIZE = Integer.parseInt(batchCfg);
        }
        // query details by batches
        List<ResourceTagJoinVO> uvList = new ArrayList<ResourceTagJoinVO>();
        // query details by batches
        int curr_index = 0;
        if (tagIds.length > DETAILS_BATCH_SIZE) {
            while ((curr_index + DETAILS_BATCH_SIZE) <= tagIds.length) {
                Long[] ids = new Long[DETAILS_BATCH_SIZE];
                for (int k = 0, j = curr_index; j < curr_index + DETAILS_BATCH_SIZE; j++, k++) {
                    ids[k] = tagIds[j];
                }
                SearchCriteria<ResourceTagJoinVO> sc = tagSearch.create();
                sc.setParameters("idIN", ids);
                List<ResourceTagJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
                if (vms != null) {
                    uvList.addAll(vms);
                }
                curr_index += DETAILS_BATCH_SIZE;
            }
        }
        if (curr_index < tagIds.length) {
            int batch_size = (tagIds.length - curr_index);
            // set the ids value
            Long[] ids = new Long[batch_size];
            for (int k = 0, j = curr_index; j < curr_index + batch_size; j++, k++) {
                ids[k] = tagIds[j];
            }
            SearchCriteria<ResourceTagJoinVO> sc = tagSearch.create();
            sc.setParameters("idIN", ids);
            List<ResourceTagJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
            if (vms != null) {
                uvList.addAll(vms);
            }
        }
        return uvList;
    }

    @Override
    public ResourceTagJoinVO searchById(Long id) {
        SearchCriteria<ResourceTagJoinVO> sc = tagIdSearch.create();
        sc.setParameters("id", id);
        List<ResourceTagJoinVO> tags = searchIncludingRemoved(sc, null, null, false);
        if (tags != null && tags.size() > 0) {
            return tags.get(0);
        } else {
            return null;
        }
    }

    @Override
    public ResourceTagJoinVO newResourceTagView(ResourceTag vr) {
        SearchCriteria<ResourceTagJoinVO> sc = tagIdSearch.create();
        sc.setParameters("id", vr.getId());
        List<ResourceTagJoinVO> tags = searchIncludingRemoved(sc, null, null, false);
        if (tags != null && tags.size() > 0) {
            return tags.get(0);
        } else {
            return null;
        }
    }

}
