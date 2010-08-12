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

package com.cloud.dc.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.dc.AccountVlanMapVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={AccountVlanMapDao.class})
public class AccountVlanMapDaoImpl extends GenericDaoBase<AccountVlanMapVO, Long> implements AccountVlanMapDao {
    
	protected SearchBuilder<AccountVlanMapVO> AccountSearch;
	protected SearchBuilder<AccountVlanMapVO> VlanSearch;
	protected SearchBuilder<AccountVlanMapVO> AccountVlanSearch;
	
	@Override
	public List<AccountVlanMapVO> listAccountVlanMapsByAccount(long accountId) {
		SearchCriteria sc = AccountSearch.create();
    	sc.setParameters("accountId", accountId);
    	return listBy(sc);
	}
	
	@Override
	public List<AccountVlanMapVO> listAccountVlanMapsByVlan(long vlanDbId) {
		SearchCriteria sc = VlanSearch.create();
    	sc.setParameters("vlanDbId", vlanDbId);
    	return listBy(sc);
	}
	
	@Override
	public AccountVlanMapVO findAccountVlanMap(long accountId, long vlanDbId) {
		SearchCriteria sc = AccountVlanSearch.create();
		sc.setParameters("accountId", accountId);
		sc.setParameters("vlanDbId", vlanDbId);
		return findOneBy(sc);
	}
	
    public AccountVlanMapDaoImpl() {
    	AccountSearch = createSearchBuilder();
    	AccountSearch.and("accountId", AccountSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountSearch.done();
        
        VlanSearch = createSearchBuilder();
    	VlanSearch.and("vlanDbId", VlanSearch.entity().getVlanDbId(), SearchCriteria.Op.EQ);
        VlanSearch.done();
        
        AccountVlanSearch = createSearchBuilder();
        AccountVlanSearch.and("accountId", AccountVlanSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountVlanSearch.and("vlanDbId", AccountVlanSearch.entity().getVlanDbId(), SearchCriteria.Op.EQ);
        AccountVlanSearch.done();
    }
    
}
