/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.network.security.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.network.security.NetworkGroupVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={NetworkGroupDao.class})
public class NetworkGroupDaoImpl extends GenericDaoBase<NetworkGroupVO, Long> implements NetworkGroupDao {
    private SearchBuilder<NetworkGroupVO> AccountIdSearch;
    private SearchBuilder<NetworkGroupVO> AccountIdNameSearch;
    private SearchBuilder<NetworkGroupVO> AccountIdNamesSearch;

    protected NetworkGroupDaoImpl() {
        AccountIdSearch = createSearchBuilder();
        AccountIdSearch.and("accountId", AccountIdSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountIdSearch.done();
        
        AccountIdNameSearch = createSearchBuilder();
        AccountIdNameSearch.and("accountId", AccountIdNameSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountIdNameSearch.and("groupName", AccountIdNameSearch.entity().getName(), SearchCriteria.Op.EQ);
        
        AccountIdNamesSearch = createSearchBuilder();
        AccountIdNamesSearch.and("accountId", AccountIdNamesSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountIdNamesSearch.and("groupNames", AccountIdNamesSearch.entity().getName(), SearchCriteria.Op.IN);
        AccountIdNameSearch.done();
    }

    @Override
    public List<NetworkGroupVO> listByAccountId(long accountId) {
        SearchCriteria<NetworkGroupVO> sc = AccountIdSearch.create();
        sc.setParameters("accountId", accountId);
        return listActiveBy(sc);
    }

    @Override
    public boolean isNameInUse(Long accountId, Long domainId, String name) {
        SearchCriteria<NetworkGroupVO> sc = createSearchCriteria();
        sc.addAnd("name", SearchCriteria.Op.EQ, name);
        if (accountId != null) {
            sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        } else {
            sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
            sc.addAnd("accountId", SearchCriteria.Op.NULL);
        }

        List<NetworkGroupVO> securityGroups = listActiveBy(sc);
        return ((securityGroups != null) && !securityGroups.isEmpty());
    }

	@Override
	public NetworkGroupVO findByAccountAndName(Long accountId, String name) {
		SearchCriteria<NetworkGroupVO> sc = AccountIdNameSearch.create();
		sc.setParameters("accountId", accountId);
		sc.setParameters("groupName", name);

		return findOneBy(sc);
	}

	@Override
	public List<NetworkGroupVO> findByAccountAndNames(Long accountId, String... names) {
		SearchCriteria<NetworkGroupVO> sc = AccountIdNamesSearch.create();
		sc.setParameters("accountId", accountId);

		sc.setParameters("groupNames", (Object [])names);

		return listActiveBy(sc);
	}
}
