/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.schedule.dao;

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.schedule.ResourceScheduleVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class ResourceScheduleDaoImpl extends GenericDaoBase<ResourceScheduleVO, Long> implements ResourceScheduleDao {

    private final SearchBuilder<ResourceScheduleVO> activeScheduleSearch;
    private final SearchBuilder<ResourceScheduleVO> allSearch;

    static final String RESOURCE_TYPE = "resourceType";
    static final String RESOURCE_ID = "resourceId";

    public ResourceScheduleDaoImpl() {
        super();

        activeScheduleSearch = createSearchBuilder();
        activeScheduleSearch.and(RESOURCE_TYPE, activeScheduleSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
        activeScheduleSearch.and(ApiConstants.ENABLED, activeScheduleSearch.entity().getEnabled(), SearchCriteria.Op.EQ);
        activeScheduleSearch.and().op(activeScheduleSearch.entity().getEndDate(), SearchCriteria.Op.NULL);
        activeScheduleSearch.or(ApiConstants.END_DATE, activeScheduleSearch.entity().getEndDate(), SearchCriteria.Op.GT);
        activeScheduleSearch.cp();
        activeScheduleSearch.done();

        allSearch = createSearchBuilder();
        allSearch.and(ApiConstants.ID, allSearch.entity().getId(), SearchCriteria.Op.IN);
        allSearch.and(RESOURCE_TYPE, allSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
        allSearch.and(RESOURCE_ID, allSearch.entity().getResourceId(), SearchCriteria.Op.EQ);
        allSearch.and(ApiConstants.ACTION, allSearch.entity().getActionName(), SearchCriteria.Op.EQ);
        allSearch.and(ApiConstants.ENABLED, allSearch.entity().getEnabled(), SearchCriteria.Op.EQ);
        allSearch.done();
    }

    @Override
    public List<ResourceScheduleVO> listAllActiveSchedules(ApiCommandResourceType resourceType) {
        SearchCriteria<ResourceScheduleVO> sc = activeScheduleSearch.create();
        sc.setParameters(RESOURCE_TYPE, resourceType);
        sc.setParameters(ApiConstants.ENABLED, true);
        sc.setParameters(ApiConstants.END_DATE, new Date());
        return search(sc, null);
    }

    @Override
    public long removeSchedulesForResourceAndIds(ApiCommandResourceType resourceType, long resourceId, List<Long> ids) {
        SearchCriteria<ResourceScheduleVO> sc = allSearch.create();
        if (CollectionUtils.isNotEmpty(ids)) {
            sc.setParameters(ApiConstants.ID, ids.toArray());
        }
        sc.setParameters(RESOURCE_TYPE, resourceType);
        sc.setParameters(RESOURCE_ID, resourceId);
        return remove(sc);
    }

    @Override
    public long removeAllSchedulesForResource(ApiCommandResourceType resourceType, long resourceId) {
        SearchCriteria<ResourceScheduleVO> sc = allSearch.create();
        sc.setParameters(RESOURCE_TYPE, resourceType);
        sc.setParameters(RESOURCE_ID, resourceId);
        return remove(sc);
    }

    @Override
    public Pair<List<ResourceScheduleVO>, Integer> searchAndCount(List<Long> ids, ApiCommandResourceType resourceType, Long resourceId,
                                                                  String action, Boolean enabled, Long offset, Long limit) {
        SearchCriteria<ResourceScheduleVO> sc = allSearch.create();
        if (CollectionUtils.isNotEmpty(ids)) {
            sc.setParameters(ApiConstants.ID, ids.toArray());
        }
        sc.setParametersIfNotNull(ApiConstants.ENABLED, enabled);
        sc.setParametersIfNotNull(ApiConstants.ACTION, action);
        sc.setParametersIfNotNull(RESOURCE_TYPE, resourceType);
        sc.setParametersIfNotNull(RESOURCE_ID, resourceId);
        Filter filter = new Filter(ResourceScheduleVO.class, ApiConstants.ID, false, offset, limit);
        return searchAndCount(sc, filter);
    }

    @Override
    public SearchCriteria<ResourceScheduleVO> getSearchCriteriaForResource(ApiCommandResourceType resourceType, long resourceId) {
        SearchCriteria<ResourceScheduleVO> sc = allSearch.create();
        sc.setParameters(RESOURCE_TYPE, resourceType);
        sc.setParameters(RESOURCE_ID, resourceId);
        return sc;
    }
}
