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

import java.util.Date;
import java.util.List;

import org.apache.cloudstack.resourcealert.vo.ResourceAlertVO;
import org.apache.commons.lang3.StringUtils;

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class ResourceAlertDaoImpl extends GenericDaoBase<ResourceAlertVO, Long> implements ResourceAlertDao {

    private final SearchBuilder<ResourceAlertVO> alertRuleIdSearch;

    public ResourceAlertDaoImpl() {
        alertRuleIdSearch = createSearchBuilder();
        alertRuleIdSearch.and("alertRuleId", alertRuleIdSearch.entity().getAlertRuleId(), SearchCriteria.Op.EQ);
        alertRuleIdSearch.done();
    }

    @Override
    public List<ResourceAlertVO> listByAlertRuleId(long alertRuleId) {
        SearchCriteria<ResourceAlertVO> sc = alertRuleIdSearch.create();
        sc.setParameters("alertRuleId", alertRuleId);
        return listBy(sc);
    }

    @Override
    public ResourceAlertVO findLastFiredForRule(long alertRuleId, Long resourceId) {
        SearchBuilder<ResourceAlertVO> sb = createSearchBuilder();
        sb.and("alertRuleId", sb.entity().getAlertRuleId(), SearchCriteria.Op.EQ);
        if (resourceId != null) {
            sb.and("resourceId", sb.entity().getResourceId(), SearchCriteria.Op.EQ);
        }
        Filter filter = new Filter(ResourceAlertVO.class, "alertTimestamp", false, 0L, 1L);
        SearchCriteria<ResourceAlertVO> sc = sb.create();
        sc.setParameters("alertRuleId", alertRuleId);
        if (resourceId != null) {
            sc.setParameters("resourceId", resourceId);
        }
        List<ResourceAlertVO> results = listBy(sc, filter);
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<ResourceAlertVO> listByFilters(Long alertRuleId, Long resourceId, String severity, Date startDate, Date endDate) {
        SearchBuilder<ResourceAlertVO> sb = createSearchBuilder();
        if (alertRuleId != null) {
            sb.and("alertRuleId", sb.entity().getAlertRuleId(), SearchCriteria.Op.EQ);
        }
        if (resourceId != null) {
            sb.and("resourceId", sb.entity().getResourceId(), SearchCriteria.Op.EQ);
        }
        if (StringUtils.isNotBlank(severity)) {
            sb.and("severity", sb.entity().getSeverity(), SearchCriteria.Op.EQ);
        }
        if (startDate != null) {
            sb.and("startDate", sb.entity().getAlertTimestamp(), SearchCriteria.Op.GTEQ);
        }
        if (endDate != null) {
            sb.and("endDate", sb.entity().getAlertTimestamp(), SearchCriteria.Op.LTEQ);
        }
        SearchCriteria<ResourceAlertVO> sc = sb.create();
        if (alertRuleId != null) sc.setParameters("alertRuleId", alertRuleId);
        if (resourceId != null) sc.setParameters("resourceId", resourceId);
        if (StringUtils.isNotBlank(severity)) sc.setParameters("severity", severity);
        if (startDate != null) sc.setParameters("startDate", startDate);
        if (endDate != null) sc.setParameters("endDate", endDate);
        return listBy(sc);
    }
}
