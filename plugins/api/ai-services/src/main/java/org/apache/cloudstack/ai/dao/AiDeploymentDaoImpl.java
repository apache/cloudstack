// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements. See the NOTICE file distributed with this
// work for additional information regarding copyright ownership. The ASF
// licenses this file to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance with the
// License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.ai.dao;

import java.util.List;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

/** The ownership DAO, built on CloudStack's own search machinery. */
public class AiDeploymentDaoImpl
        extends GenericDaoBase<AiDeploymentVO, Long> implements AiDeploymentDao {

    private final SearchBuilder<AiDeploymentVO> byAccount;
    private final SearchBuilder<AiDeploymentVO> byDomain;
    private final SearchBuilder<AiDeploymentVO> byZone;
    private final SearchBuilder<AiDeploymentVO> byEngineId;

    public AiDeploymentDaoImpl() {
        byAccount = createSearchBuilder();
        byAccount.and("accountId", byAccount.entity().getAccountId(), SearchCriteria.Op.EQ);
        byAccount.done();

        byDomain = createSearchBuilder();
        byDomain.and("domainId", byDomain.entity().getDomainId(), SearchCriteria.Op.EQ);
        byDomain.done();

        byZone = createSearchBuilder();
        byZone.and("zoneId", byZone.entity().getZoneId(), SearchCriteria.Op.EQ);
        byZone.done();

        byEngineId = createSearchBuilder();
        byEngineId.and("engineId", byEngineId.entity().getEngineId(), SearchCriteria.Op.EQ);
        byEngineId.done();
    }

    @Override
    public List<AiDeploymentVO> listByAccount(long accountId) {
        SearchCriteria<AiDeploymentVO> search = byAccount.create();
        search.setParameters("accountId", accountId);
        return listBy(search);
    }

    @Override
    public List<AiDeploymentVO> listByDomain(long domainId) {
        SearchCriteria<AiDeploymentVO> search = byDomain.create();
        search.setParameters("domainId", domainId);
        return listBy(search);
    }

    @Override
    public List<AiDeploymentVO> listByZone(long zoneId) {
        SearchCriteria<AiDeploymentVO> search = byZone.create();
        search.setParameters("zoneId", zoneId);
        return listBy(search);
    }

    @Override
    public AiDeploymentVO findByEngineId(String engineId) {
        SearchCriteria<AiDeploymentVO> search = byEngineId.create();
        search.setParameters("engineId", engineId);
        return findOneBy(search);
    }

    @Override
    public int removeByAccount(long accountId) {
        SearchCriteria<AiDeploymentVO> search = byAccount.create();
        search.setParameters("accountId", accountId);
        return remove(search);
    }
}
