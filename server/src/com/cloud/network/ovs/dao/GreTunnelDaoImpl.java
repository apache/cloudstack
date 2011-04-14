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

@Local(value = { GreTunnelDao.class })
public class GreTunnelDaoImpl extends GenericDaoBase<GreTunnelVO, Long>
		implements GreTunnelDao {
	protected final SearchBuilder<GreTunnelVO> fromSearch;
	protected final SearchBuilder<GreTunnelVO> fromToSearch;
	
	public GreTunnelDaoImpl() {
		fromSearch = createSearchBuilder();
		fromSearch.and("from", fromSearch.entity().getFrom(), Op.EQ);
		fromSearch.done();
		
		fromToSearch = createSearchBuilder();
		fromToSearch.and("from", fromToSearch.entity().getFrom(), Op.EQ);
		fromToSearch.and("to", fromToSearch.entity().getTo(), Op.EQ);
		fromToSearch.done();
	}
	
	@Override
	public List<GreTunnelVO> getByFrom(long from) {
		SearchCriteria<GreTunnelVO> sc = fromSearch.create();
        sc.setParameters("from", from);
		return listBy(sc, null);
	}

	@Override
	public GreTunnelVO getByFromAndTo(long from, long to) {
		SearchCriteria<GreTunnelVO> sc = fromToSearch.create();
        sc.setParameters("from", from);
        sc.setParameters("to", to);
		return findOneBy(sc);
	}
	
	@Override
	public GreTunnelVO lockByFromAndTo(long from, long to) {
		SearchCriteria<GreTunnelVO> sc = fromToSearch.create();
        sc.setParameters("from", from);
        sc.setParameters("to", to);
        return lockOneRandomRow(sc, true);
	}

}
