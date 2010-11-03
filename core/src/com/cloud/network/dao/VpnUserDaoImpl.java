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

package com.cloud.network.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.network.VpnUserVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={VpnUserDao.class})
public class VpnUserDaoImpl extends GenericDaoBase<VpnUserVO, Long> implements VpnUserDao {
    private final SearchBuilder<VpnUserVO> AccountSearch;
    private final SearchBuilder<VpnUserVO> AccountNameSearch;


    protected VpnUserDaoImpl() {

        AccountSearch = createSearchBuilder();
        AccountSearch.and("accountId", AccountSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountSearch.done();
        
        AccountNameSearch = createSearchBuilder();
        AccountNameSearch.and("accountId", AccountNameSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountNameSearch.and("userName", AccountNameSearch.entity().getUserName(), SearchCriteria.Op.EQ);
        AccountNameSearch.done();
    }

    @Override
    public List<VpnUserVO> listByAccount(Long accountId) {
        SearchCriteria<VpnUserVO> sc = AccountSearch.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc);
    }

	@Override
	public VpnUserVO findByAccountAndUsername(Long accountId, String userName) {
		SearchCriteria<VpnUserVO> sc = AccountSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("userName", userName);

        return findOneBy(sc);
	}
}
