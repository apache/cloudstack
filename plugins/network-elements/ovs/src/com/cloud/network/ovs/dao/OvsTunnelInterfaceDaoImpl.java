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

@Local(value = { OvsTunnelInterfaceDao.class })
public class OvsTunnelInterfaceDaoImpl extends
		GenericDaoBase<OvsTunnelInterfaceVO, Long> implements OvsTunnelInterfaceDao {

	protected final SearchBuilder<OvsTunnelInterfaceVO> hostAndLabelSearch;
	protected final SearchBuilder<OvsTunnelInterfaceVO> labelSearch;
	
	public OvsTunnelInterfaceDaoImpl() {
		hostAndLabelSearch = createSearchBuilder();
		hostAndLabelSearch.and("host_id", hostAndLabelSearch.entity().getHostId(), Op.EQ);
		hostAndLabelSearch.and("label", hostAndLabelSearch.entity().getLabel(), Op.EQ);
		hostAndLabelSearch.done();
		
		labelSearch = createSearchBuilder();
		labelSearch.and("label", labelSearch.entity().getLabel(), Op.EQ);
		labelSearch.done();
		
	}
	
	@Override
	public OvsTunnelInterfaceVO getByHostAndLabel(long hostId, String label) {
		SearchCriteria<OvsTunnelInterfaceVO> sc = hostAndLabelSearch.create();
        sc.setParameters("host_id", hostId);
        sc.setParameters("label", label);
		return findOneBy(sc);
	}

    @Override
    public List<OvsTunnelInterfaceVO> listByLabel(String label) {
        SearchCriteria<OvsTunnelInterfaceVO> sc = labelSearch.create();
        sc.setParameters("label", label);
        return listBy(sc);
    }


}
