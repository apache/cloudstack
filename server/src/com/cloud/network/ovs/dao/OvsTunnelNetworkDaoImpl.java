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

@Local(value = { OvsTunnelNetworkDao.class })
public class OvsTunnelNetworkDaoImpl extends
		GenericDaoBase<OvsTunnelNetworkVO, Long> implements OvsTunnelNetworkDao {

	protected final SearchBuilder<OvsTunnelNetworkVO> fromToNetworkSearch;
	protected final SearchBuilder<OvsTunnelNetworkVO> fromNetworkSearch;
	protected final SearchBuilder<OvsTunnelNetworkVO> toNetworkSearch;
	
	public OvsTunnelNetworkDaoImpl() {
		fromToNetworkSearch = createSearchBuilder();
		fromToNetworkSearch.and("from", fromToNetworkSearch.entity().getFrom(), Op.EQ);
		fromToNetworkSearch.and("to", fromToNetworkSearch.entity().getTo(), Op.EQ);
		fromToNetworkSearch.and("network_id", fromToNetworkSearch.entity().getNetworkId(), Op.EQ);
		fromToNetworkSearch.done();
		
		fromNetworkSearch = createSearchBuilder();
		fromNetworkSearch.and("from", fromNetworkSearch.entity().getFrom(), Op.EQ);
		fromNetworkSearch.and("network_id", fromNetworkSearch.entity().getNetworkId(), Op.EQ);
		fromNetworkSearch.done();
		
		toNetworkSearch = createSearchBuilder();
		toNetworkSearch.and("to", toNetworkSearch.entity().getTo(), Op.EQ);
		toNetworkSearch.and("network_id", toNetworkSearch.entity().getNetworkId(), Op.EQ);
		toNetworkSearch.done();
	}
	
	@Override
	public OvsTunnelNetworkVO getByFromToNetwork(long from, long to,
			long networkId) {
		SearchCriteria<OvsTunnelNetworkVO> sc = fromToNetworkSearch.create();
        sc.setParameters("from", from);
        sc.setParameters("to", to);
        sc.setParameters("network_id", networkId);
		return findOneBy(sc);
	}

    @Override
    public void removeByFromNetwork(long from, long networkId) {
        SearchCriteria<OvsTunnelNetworkVO> sc = fromNetworkSearch.create();
        sc.setParameters("from", from);
        sc.setParameters("network_id", networkId);
        remove(sc);
    }

    @Override
    public List<OvsTunnelNetworkVO> listByToNetwork(long to, long networkId) {
        SearchCriteria<OvsTunnelNetworkVO> sc = toNetworkSearch.create();
        sc.setParameters("to", to);
        sc.setParameters("network_id", networkId);
        return listBy(sc);
    }

    @Override
    public void removeByFromToNetwork(long from, long to, long networkId) {
        SearchCriteria<OvsTunnelNetworkVO> sc = fromToNetworkSearch.create();
        sc.setParameters("from", from);
        sc.setParameters("to", to);
        sc.setParameters("network_id", networkId);
        remove(sc);
    }

}
