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
package com.cloud.network.security.dao;

import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloud.network.security.SecurityGroupVO;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.tags.dao.ResourceTagsDaoImpl;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Component
@Local(value={SecurityGroupDao.class})
public class SecurityGroupDaoImpl extends GenericDaoBase<SecurityGroupVO, Long> implements SecurityGroupDao {
    private SearchBuilder<SecurityGroupVO> AccountIdSearch;
    private SearchBuilder<SecurityGroupVO> AccountIdNameSearch;
    private SearchBuilder<SecurityGroupVO> AccountIdNamesSearch;
    @Inject ResourceTagDao _tagsDao;


    protected SecurityGroupDaoImpl() {
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
    public List<SecurityGroupVO> listByAccountId(long accountId) {
        SearchCriteria<SecurityGroupVO> sc = AccountIdSearch.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc);
    }

    @Override
    public boolean isNameInUse(Long accountId, Long domainId, String name) {
        SearchCriteria<SecurityGroupVO> sc = createSearchCriteria();
        sc.addAnd("name", SearchCriteria.Op.EQ, name);
        if (accountId != null) {
            sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        } else {
            sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
            sc.addAnd("accountId", SearchCriteria.Op.NULL);
        }

        List<SecurityGroupVO> securityGroups = listBy(sc);
        return ((securityGroups != null) && !securityGroups.isEmpty());
    }

	@Override
	public SecurityGroupVO findByAccountAndName(Long accountId, String name) {
		SearchCriteria<SecurityGroupVO> sc = AccountIdNameSearch.create();
		sc.setParameters("accountId", accountId);
		sc.setParameters("name", name);

		return findOneIncludingRemovedBy(sc);
	}

	@Override
	public List<SecurityGroupVO> findByAccountAndNames(Long accountId, String... names) {
		SearchCriteria<SecurityGroupVO> sc = AccountIdNamesSearch.create();
		sc.setParameters("accountId", accountId);

		sc.setParameters("groupNames", (Object [])names);

		return listBy(sc);
	}
	@Override
	public int removeByAccountId(long accountId) {
	    SearchCriteria<SecurityGroupVO> sc = AccountIdSearch.create();
	    sc.setParameters("accountId", accountId);
	    return expunge(sc);
	} 

	
	@Override
    @DB
    public boolean remove(Long id) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        SecurityGroupVO entry = findById(id);
        if (entry != null) {
            _tagsDao.removeByIdAndType(id, TaggedResourceType.SecurityGroup);
        }
        boolean result = super.remove(id);
        txn.commit();
        return result;
    }
	
	@Override
    @DB
    public boolean expunge(Long id) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        SecurityGroupVO entry = findById(id);
        if (entry != null) {
            _tagsDao.removeByIdAndType(id, TaggedResourceType.SecurityGroup);
        }
        boolean result = super.expunge(id);
        txn.commit();
        return result;
    }
}
