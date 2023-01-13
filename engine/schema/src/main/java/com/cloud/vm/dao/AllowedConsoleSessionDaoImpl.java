//
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
//

package com.cloud.vm.dao;

import com.cloud.vm.AllowedConsoleSessionVo;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class AllowedConsoleSessionDaoImpl extends GenericDaoBase<AllowedConsoleSessionVo, Long> implements AllowedConsoleSessionDao {

    private final SearchBuilder<AllowedConsoleSessionVo> searchBySessionUuids;

    protected AllowedConsoleSessionDaoImpl() {
        searchBySessionUuids = createSearchBuilder();
        searchBySessionUuids.and("sessions", searchBySessionUuids.entity().getSessionUuid(), SearchCriteria.Op.IN);
        searchBySessionUuids.done();
    }

    @Override
    public void expungeBySessionUuids(Object[] sessionsUuids) {
        expunge(getSearchCriteriaBySessionUuids(sessionsUuids));
    }

    @Override
    public boolean isSessionUuidAllowed(String sessionUuid) {
        return findOneBy(getSearchCriteriaBySessionUuids(new String[]{sessionUuid})) != null;
    }

    private SearchCriteria<AllowedConsoleSessionVo> getSearchCriteriaBySessionUuids(Object[] sessionsUuids) {
        SearchCriteria<AllowedConsoleSessionVo> sc = searchBySessionUuids.create();
        sc.setParameters("sessions", sessionsUuids);
        return sc;
    }
}
