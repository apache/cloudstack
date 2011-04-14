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

package com.cloud.network.ovs.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Local(value = { OvsTunnelAccountDao.class })
public class OvsTunnelAccountDaoImpl extends
		GenericDaoBase<OvsTunnelAccountVO, Long> implements OvsTunnelAccountDao {

	protected final SearchBuilder<OvsTunnelAccountVO> fromToAccountSearch;
	protected final SearchBuilder<OvsTunnelAccountVO> fromAccountSearch;
	protected final SearchBuilder<OvsTunnelAccountVO> toAccountSearch;
	
	public OvsTunnelAccountDaoImpl() {
		fromToAccountSearch = createSearchBuilder();
		fromToAccountSearch.and("from", fromToAccountSearch.entity().getFrom(), Op.EQ);
		fromToAccountSearch.and("to", fromToAccountSearch.entity().getTo(), Op.EQ);
		fromToAccountSearch.and("account", fromToAccountSearch.entity().getAccount(), Op.EQ);
		fromToAccountSearch.done();
		
		fromAccountSearch = createSearchBuilder();
		fromAccountSearch.and("from", fromAccountSearch.entity().getFrom(), Op.EQ);
		fromAccountSearch.and("account", fromAccountSearch.entity().getAccount(), Op.EQ);
		fromAccountSearch.done();
		
		toAccountSearch = createSearchBuilder();
		toAccountSearch.and("to", toAccountSearch.entity().getTo(), Op.EQ);
		toAccountSearch.and("account", toAccountSearch.entity().getAccount(), Op.EQ);
		toAccountSearch.done();
	}
	
	@Override
	public OvsTunnelAccountVO getByFromToAccount(long from, long to,
			long account) {
		SearchCriteria<OvsTunnelAccountVO> sc = fromToAccountSearch.create();
        sc.setParameters("from", from);
        sc.setParameters("to", to);
        sc.setParameters("account", account);
		return findOneBy(sc);
	}

    @Override
    public void removeByFromAccount(long from, long account) {
        SearchCriteria<OvsTunnelAccountVO> sc = fromAccountSearch.create();
        sc.setParameters("from", from);
        sc.setParameters("account", account);
        remove(sc);
    }

    @Override
    public List<OvsTunnelAccountVO> listByToAccount(long to, long account) {
        SearchCriteria<OvsTunnelAccountVO> sc = toAccountSearch.create();
        sc.setParameters("to", to);
        sc.setParameters("account", account);
        return listBy(sc);
    }

    @Override
    public void removeByFromToAccount(long from, long to, long account) {
        SearchCriteria<OvsTunnelAccountVO> sc = fromToAccountSearch.create();
        sc.setParameters("from", from);
        sc.setParameters("to", to);
        sc.setParameters("account", account);
        remove(sc);
    }

}
