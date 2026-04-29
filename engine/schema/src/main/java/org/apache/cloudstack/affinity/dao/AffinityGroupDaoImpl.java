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
import javax.inject.Inject;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.affinity.AffinityGroupDomainMapVO;
import org.apache.cloudstack.affinity.AffinityGroupVO;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class AffinityGroupDaoImpl extends GenericDaoBase<AffinityGroupVO, Long> implements AffinityGroupDao {
    private SearchBuilder<AffinityGroupVO> IdsSearch;
    private SearchBuilder<AffinityGroupVO> AccountIdSearch;
    private SearchBuilder<AffinityGroupVO> AccountIdNameSearch;
    private SearchBuilder<AffinityGroupVO> AccountIdNamesSearch;
    private SearchBuilder<AffinityGroupVO> DomainLevelNameSearch;
    private SearchBuilder<AffinityGroupVO> AccountIdTypeSearch;
    @Inject
    AffinityGroupDomainMapDao _groupDomainDao;

    private SearchBuilder<AffinityGroupVO> DomainLevelTypeSearch;

    public AffinityGroupDaoImpl() {

    }

    @PostConstruct
    protected void init() {
        IdsSearch = createSearchBuilder();
        IdsSearch.and("idIn", IdsSearch.entity().getId(), SearchCriteria.Op.IN);
        IdsSearch.done();

        AccountIdSearch = createSearchBuilder();
        AccountIdSearch.and("accountId", AccountIdSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountIdSearch.done();

        AccountIdNameSearch = createSearchBuilder();
        AccountIdNameSearch.and("accountId", AccountIdNameSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountIdNameSearch.and("name", AccountIdNameSearch.entity().getName(), SearchCriteria.Op.EQ);

        AccountIdNamesSearch = createSearchBuilder();
        AccountIdNamesSearch.and("accountId", AccountIdNamesSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountIdNamesSearch.and("groupNames", AccountIdNamesSearch.entity().getName(), SearchCriteria.Op.IN);
        AccountIdNameSearch.done();

        SearchBuilder<AffinityGroupDomainMapVO> domainMapSearch = _groupDomainDao.createSearchBuilder();
        domainMapSearch.and("domainId", domainMapSearch.entity().getDomainId(), SearchCriteria.Op.EQ);

        DomainLevelNameSearch = createSearchBuilder();
        DomainLevelNameSearch.and("name", DomainLevelNameSearch.entity().getName(), SearchCriteria.Op.EQ);
        DomainLevelNameSearch.and("aclType", DomainLevelNameSearch.entity().getAclType(), SearchCriteria.Op.EQ);
        DomainLevelNameSearch.join("domainMapSearch", domainMapSearch, domainMapSearch.entity().getAffinityGroupId(), DomainLevelNameSearch.entity().getId(),
            JoinType.INNER);
        DomainLevelNameSearch.done();

        AccountIdTypeSearch = createSearchBuilder();
        AccountIdTypeSearch.and("accountId", AccountIdTypeSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountIdTypeSearch.and("type", AccountIdTypeSearch.entity().getType(), SearchCriteria.Op.EQ);

        SearchBuilder<AffinityGroupDomainMapVO> domainTypeSearch = _groupDomainDao.createSearchBuilder();
        domainTypeSearch.and("domainId", domainTypeSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        DomainLevelTypeSearch = createSearchBuilder();
        DomainLevelTypeSearch.and("type", DomainLevelTypeSearch.entity().getType(), SearchCriteria.Op.EQ);
        DomainLevelTypeSearch.and("aclType", DomainLevelTypeSearch.entity().getAclType(), SearchCriteria.Op.EQ);
        DomainLevelTypeSearch.join("domainTypeSearch", domainTypeSearch, domainTypeSearch.entity().getAffinityGroupId(), DomainLevelTypeSearch.entity().getId(),
            JoinType.INNER);
        DomainLevelTypeSearch.done();
    }

    @Override
    public List<AffinityGroupVO> listByAccountId(long accountId) {
        SearchCriteria<AffinityGroupVO> sc = AccountIdSearch.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc);
    }

    @Override
    public boolean isNameInUse(Long accountId, Long domainId, String name) {
        SearchCriteria<AffinityGroupVO> sc = createSearchCriteria();
        sc.addAnd("name", SearchCriteria.Op.EQ, name);
        if (accountId != null) {
            sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        } else {
            sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
            sc.addAnd("accountId", SearchCriteria.Op.NULL);
        }

        List<AffinityGroupVO> AffinityGroups = listBy(sc);
        return ((AffinityGroups != null) && !AffinityGroups.isEmpty());
    }

    @Override
    public AffinityGroupVO findByAccountAndName(Long accountId, String name) {
        SearchCriteria<AffinityGroupVO> sc = AccountIdNameSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("name", name);

        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public List<AffinityGroupVO> findByAccountAndNames(Long accountId, String... names) {
        SearchCriteria<AffinityGroupVO> sc = AccountIdNamesSearch.create();
        sc.setParameters("accountId", accountId);

        sc.setParameters("groupNames", (Object[])names);

        return listBy(sc);
    }

    @Override
    public int removeByAccountId(long accountId) {
        SearchCriteria<AffinityGroupVO> sc = AccountIdSearch.create();
        sc.setParameters("accountId", accountId);
        return expunge(sc);
    }

    @Override
    public AffinityGroupVO findDomainLevelGroupByName(Long domainId, String affinityGroupName) {
        SearchCriteria<AffinityGroupVO> sc = DomainLevelNameSearch.create();
        sc.setParameters("aclType", ControlledEntity.ACLType.Domain);
        sc.setParameters("name", affinityGroupName);
        sc.setJoinParameters("domainMapSearch", "domainId", domainId);
        return findOneBy(sc);
    }

    @Override
    public AffinityGroupVO findByAccountAndType(Long accountId, String type) {
        SearchCriteria<AffinityGroupVO> sc = AccountIdTypeSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("type", type);

        return findOneBy(sc);
    }

    @Override
    public AffinityGroupVO findDomainLevelGroupByType(Long domainId, String type) {
        SearchCriteria<AffinityGroupVO> sc = DomainLevelTypeSearch.create();
        sc.setParameters("aclType", ControlledEntity.ACLType.Domain);
        sc.setParameters("type", type);
        sc.setJoinParameters("domainTypeSearch", "domainId", domainId);
        return findOneBy(sc);
    }

    @Override
    public List<AffinityGroupVO> listByIds(List<Long> ids, boolean exclusive) {
        SearchCriteria<AffinityGroupVO> sc = IdsSearch.create();
        sc.setParameters("idIn", ids.toArray());
        return lockRows(sc, null, exclusive);
    }
}
