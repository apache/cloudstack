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
import javax.ejb.Local;
import org.apache.cloudstack.affinity.AffinityGroupVO;
import org.springframework.stereotype.Component;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value = { AffinityGroupDao.class })
public class AffinityGroupDaoImpl extends GenericDaoBase<AffinityGroupVO, Long> implements AffinityGroupDao {
    private SearchBuilder<AffinityGroupVO> AccountIdSearch;
    private SearchBuilder<AffinityGroupVO> AccountIdNameSearch;
    private SearchBuilder<AffinityGroupVO> AccountIdNamesSearch;


    public AffinityGroupDaoImpl() {

    }

    @PostConstruct
    protected void init() {
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

		sc.setParameters("groupNames", (Object [])names);

		return listBy(sc);
	}
	@Override
	public int removeByAccountId(long accountId) {
        SearchCriteria<AffinityGroupVO> sc = AccountIdSearch.create();
	    sc.setParameters("accountId", accountId);
	    return expunge(sc);
	}
}
