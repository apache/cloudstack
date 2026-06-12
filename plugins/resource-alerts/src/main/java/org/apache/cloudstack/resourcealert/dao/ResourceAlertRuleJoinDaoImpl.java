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

package org.apache.cloudstack.resourcealert.dao;

import java.util.List;

import org.apache.cloudstack.resourcealert.vo.ResourceAlertRuleJoinVO;
import org.apache.commons.lang3.StringUtils;

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class ResourceAlertRuleJoinDaoImpl extends GenericDaoBase<ResourceAlertRuleJoinVO, Long> implements ResourceAlertRuleJoinDao {

    @Override
    public ResourceAlertRuleJoinVO findByUuid(String uuid) {
        SearchBuilder<ResourceAlertRuleJoinVO> sb = createSearchBuilder();
        sb.and("uuid", sb.entity().getUuid(), SearchCriteria.Op.EQ);
        SearchCriteria<ResourceAlertRuleJoinVO> sc = sb.create();
        sc.setParameters("uuid", uuid);
        return findOneBy(sc);
    }

    @Override
    public List<ResourceAlertRuleJoinVO> searchByFilters(Long id, String name, String resourceType, Long resourceId,
            String accountName, Long domainId, Long offset, Long limit) {
        SearchCriteria<ResourceAlertRuleJoinVO> sc = buildFilterCriteria(id, name, resourceType, resourceId, accountName, domainId);
        Filter filter = new Filter(ResourceAlertRuleJoinVO.class, "id", true, offset, limit);
        return listBy(sc, filter);
    }

    @Override
    public int countByFilters(Long id, String name, String resourceType, Long resourceId,
            String accountName, Long domainId) {
        SearchCriteria<ResourceAlertRuleJoinVO> sc = buildFilterCriteria(id, name, resourceType, resourceId, accountName, domainId);
        return getCount(sc);
    }

    private SearchCriteria<ResourceAlertRuleJoinVO> buildFilterCriteria(Long id, String name, String resourceType,
            Long resourceId, String accountName, Long domainId) {
        SearchBuilder<ResourceAlertRuleJoinVO> sb = createSearchBuilder();
        if (id != null) sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        if (StringUtils.isNotBlank(name)) sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        if (StringUtils.isNotBlank(resourceType)) sb.and("resourceType", sb.entity().getResourceType(), SearchCriteria.Op.EQ);
        if (resourceId != null) sb.and("resourceId", sb.entity().getResourceId(), SearchCriteria.Op.EQ);
        if (StringUtils.isNotBlank(accountName)) sb.and("accountName", sb.entity().getAccountName(), SearchCriteria.Op.EQ);
        if (domainId != null) sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
        // exclude soft-deleted rules
        sb.and("removed", sb.entity().getRemoved(), SearchCriteria.Op.NULL);

        SearchCriteria<ResourceAlertRuleJoinVO> sc = sb.create();
        if (id != null) sc.setParameters("id", id);
        if (StringUtils.isNotBlank(name)) sc.setParameters("name", name);
        if (StringUtils.isNotBlank(resourceType)) sc.setParameters("resourceType", resourceType);
        if (resourceId != null) sc.setParameters("resourceId", resourceId);
        if (StringUtils.isNotBlank(accountName)) sc.setParameters("accountName", accountName);
        if (domainId != null) sc.setParameters("domainId", domainId);
        return sc;
    }
}
