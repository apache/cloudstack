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

package com.cloud.network.dao;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class RouterHealthCheckResultDaoImpl extends GenericDaoBase<RouterHealthCheckResultVO, Long> implements RouterHealthCheckResultDao {

    private SearchBuilder<RouterHealthCheckResultVO> RouterChecksSearchBuilder;
    private SearchBuilder<RouterHealthCheckResultVO> IsRouterFailingSearchBuilder;

    protected RouterHealthCheckResultDaoImpl() {
        super();
        RouterChecksSearchBuilder = createSearchBuilder();
        RouterChecksSearchBuilder.and("routerId", RouterChecksSearchBuilder.entity().getRouterId(), SearchCriteria.Op.EQ);
        RouterChecksSearchBuilder.and("checkName", RouterChecksSearchBuilder.entity().getCheckName(), SearchCriteria.Op.EQ);
        RouterChecksSearchBuilder.and("checkType", RouterChecksSearchBuilder.entity().getCheckType(), SearchCriteria.Op.EQ);
        RouterChecksSearchBuilder.done();

        IsRouterFailingSearchBuilder = createSearchBuilder();
        IsRouterFailingSearchBuilder.and("routerId", IsRouterFailingSearchBuilder.entity().getRouterId(), SearchCriteria.Op.EQ);
        IsRouterFailingSearchBuilder.and("checkResult", IsRouterFailingSearchBuilder.entity().getCheckResult(), SearchCriteria.Op.EQ);
        IsRouterFailingSearchBuilder.done();
    }

    @Override
    public List<RouterHealthCheckResultVO> getHealthCheckResults(long routerId) {
        SearchCriteria<RouterHealthCheckResultVO> sc = RouterChecksSearchBuilder.create();
        sc.setParameters("routerId", routerId);
        return listBy(sc);
    }

    @Override
    public boolean expungeHealthChecks(long routerId) {
        SearchCriteria<RouterHealthCheckResultVO> sc = RouterChecksSearchBuilder.create();
        sc.setParameters("routerId", routerId);
        return expunge(sc) > 0;
    }

    @Override
    public RouterHealthCheckResultVO getRouterHealthCheckResult(long routerId, String checkName, String checkType) {
        SearchCriteria<RouterHealthCheckResultVO> sc = RouterChecksSearchBuilder.create();
        sc.setParameters("routerId", routerId);
        sc.setParameters("checkName", checkName);
        sc.setParameters("checkType", checkType);
        List<RouterHealthCheckResultVO> checks = listBy(sc);
        if (checks.size() > 1) {
            logger.error("Found multiple entries for router Id: " + routerId + ", check name: " + checkName);
        }
        return checks.isEmpty() ? null : checks.get(0);
    }

    @Override
    public boolean hasFailingChecks(long routerId) {
        SearchCriteria<RouterHealthCheckResultVO> sc = IsRouterFailingSearchBuilder.create();
        sc.setParameters("routerId", routerId);
        sc.setParameters("checkResult", false);
        return !listBy(sc).isEmpty();
    }
}
