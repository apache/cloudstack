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

import org.apache.cloudstack.resourcealert.ResourceAlertRule;
import org.apache.cloudstack.resourcealert.vo.ResourceAlertRuleVO;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class ResourceAlertRuleDaoImpl extends GenericDaoBase<ResourceAlertRuleVO, Long> implements ResourceAlertRuleDao {

    private final SearchBuilder<ResourceAlertRuleVO> activeSearch;
    private final SearchBuilder<ResourceAlertRuleVO> accountIdSearch;
    private final SearchBuilder<ResourceAlertRuleVO> resourceTypeAndIdSearch;
    private final SearchBuilder<ResourceAlertRuleVO> activeByAccountSearch;
    private final SearchBuilder<ResourceAlertRuleVO> specificRuleSearch;

    public ResourceAlertRuleDaoImpl() {
        activeSearch = createSearchBuilder();
        activeSearch.and("removed", activeSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeSearch.done();

        accountIdSearch = createSearchBuilder();
        accountIdSearch.and("accountId", accountIdSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        accountIdSearch.done();

        resourceTypeAndIdSearch = createSearchBuilder();
        resourceTypeAndIdSearch.and("resourceType", resourceTypeAndIdSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
        resourceTypeAndIdSearch.and("resourceId", resourceTypeAndIdSearch.entity().getResourceId(), SearchCriteria.Op.EQ);
        resourceTypeAndIdSearch.done();

        activeByAccountSearch = createSearchBuilder();
        activeByAccountSearch.and("accountId", activeByAccountSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        activeByAccountSearch.and("removed", activeByAccountSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeByAccountSearch.done();

        specificRuleSearch = createSearchBuilder();
        specificRuleSearch.and("resourceType", specificRuleSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
        specificRuleSearch.and("metric", specificRuleSearch.entity().getMetric(), SearchCriteria.Op.EQ);
        specificRuleSearch.and("resourceId", specificRuleSearch.entity().getResourceId(), SearchCriteria.Op.EQ);
        specificRuleSearch.and("removed", specificRuleSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        specificRuleSearch.done();
    }

    @Override
    public List<ResourceAlertRuleVO> listActive() {
        SearchCriteria<ResourceAlertRuleVO> sc = activeSearch.create();
        return listBy(sc);
    }

    @Override
    public ResourceAlertRuleVO findByUuid(String uuid) {
        SearchBuilder<ResourceAlertRuleVO> sb = createSearchBuilder();
        sb.and("uuid", sb.entity().getUuid(), SearchCriteria.Op.EQ);
        SearchCriteria<ResourceAlertRuleVO> sc = sb.create();
        sc.setParameters("uuid", uuid);
        return findOneBy(sc);
    }

    @Override
    public List<ResourceAlertRuleVO> listByAccountId(long accountId) {
        SearchCriteria<ResourceAlertRuleVO> sc = accountIdSearch.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc);
    }

    @Override
    public List<ResourceAlertRuleVO> listByResourceTypeAndId(ResourceAlertRule.ResourceType resourceType, Long resourceId) {
        SearchCriteria<ResourceAlertRuleVO> sc = resourceTypeAndIdSearch.create();
        sc.setParameters("resourceType", resourceType);
        if (resourceId != null) {
            sc.setParameters("resourceId", resourceId);
        } else {
            sc.setParameters("resourceId", (Object) null);
        }
        return listBy(sc);
    }

    @Override
    public int countActiveByAccountId(long accountId) {
        SearchCriteria<ResourceAlertRuleVO> sc = activeByAccountSearch.create();
        sc.setParameters("accountId", accountId);
        return getCount(sc);
    }

    @Override
    public boolean existsSpecificRule(ResourceAlertRule.ResourceType resourceType, String metric, long resourceId) {
        SearchCriteria<ResourceAlertRuleVO> sc = specificRuleSearch.create();
        sc.setParameters("resourceType", resourceType);
        sc.setParameters("metric", metric);
        sc.setParameters("resourceId", resourceId);
        return getCount(sc) > 0;
    }
}
