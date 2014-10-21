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
package org.apache.cloudstack.affinity.dao;

import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.cloudstack.affinity.AffinityGroupDomainMapVO;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

public class AffinityGroupDomainMapDaoImpl extends GenericDaoBase<AffinityGroupDomainMapVO, Long> implements AffinityGroupDomainMapDao {

    private SearchBuilder<AffinityGroupDomainMapVO> ListByAffinityGroup;

    private SearchBuilder<AffinityGroupDomainMapVO> DomainsSearch;

    public AffinityGroupDomainMapDaoImpl() {
    }

    @PostConstruct
    protected void init() {
        ListByAffinityGroup = createSearchBuilder();
        ListByAffinityGroup.and("affinityGroupId", ListByAffinityGroup.entity().getAffinityGroupId(), SearchCriteria.Op.EQ);
        ListByAffinityGroup.done();

        DomainsSearch = createSearchBuilder();
        DomainsSearch.and("domainId", DomainsSearch.entity().getDomainId(), Op.IN);
        DomainsSearch.done();
    }

    @Override
    public AffinityGroupDomainMapVO findByAffinityGroup(long affinityGroupId) {
        SearchCriteria<AffinityGroupDomainMapVO> sc = ListByAffinityGroup.create();
        sc.setParameters("affinityGroupId", affinityGroupId);
        return findOneBy(sc);
    }

    @Override
    public List<AffinityGroupDomainMapVO> listByDomain(Object... domainId) {
        SearchCriteria<AffinityGroupDomainMapVO> sc = DomainsSearch.create();
        sc.setParameters("domainId", domainId);

        return listBy(sc);
    }

}
