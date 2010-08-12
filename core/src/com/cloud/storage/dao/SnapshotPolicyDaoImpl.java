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

package com.cloud.storage.dao;


import java.util.List;

import javax.ejb.Local;

import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local (value={SnapshotPolicyDao.class})
public class SnapshotPolicyDaoImpl extends GenericDaoBase<SnapshotPolicyVO, Long> implements SnapshotPolicyDao {
	private final SearchBuilder<SnapshotPolicyVO> VolumeIdSearch;
	private final SearchBuilder<SnapshotPolicyVO> VolumeIdIntervalSearch;
	private final SearchBuilder<SnapshotPolicyVO> ActivePolicySearch;
	
	@Override
	public SnapshotPolicyVO findOneByVolumeInterval(long volumeId, short interval) {
		SearchCriteria sc = VolumeIdIntervalSearch.create();
        sc.setParameters("volumeId", volumeId);
        sc.setParameters("interval", interval);
		return findOneBy(sc);
	}
	
	@Override
	public List<SnapshotPolicyVO> listByVolumeId(long volumeId) {
		return listByVolumeId(volumeId, null);
	}
	
    @Override
    public List<SnapshotPolicyVO> listByVolumeId(long volumeId, Filter filter) {
        SearchCriteria sc = VolumeIdSearch.create();
        sc.setParameters("volumeId", volumeId);
        sc.setParameters("active", true);
        return listActiveBy(sc, filter);
    }
	
    protected SnapshotPolicyDaoImpl() {
        VolumeIdSearch = createSearchBuilder();
        VolumeIdSearch.and("volumeId", VolumeIdSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        VolumeIdSearch.and("active", VolumeIdSearch.entity().isActive(), SearchCriteria.Op.EQ);
        VolumeIdSearch.done();
        
        VolumeIdIntervalSearch = createSearchBuilder();
        VolumeIdIntervalSearch.and("volumeId", VolumeIdIntervalSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        VolumeIdIntervalSearch.and("interval", VolumeIdIntervalSearch.entity().getInterval(), SearchCriteria.Op.EQ);
        VolumeIdIntervalSearch.done();
        
        ActivePolicySearch = createSearchBuilder();
        ActivePolicySearch.and("active", ActivePolicySearch.entity().isActive(), SearchCriteria.Op.EQ);
        ActivePolicySearch.done();
    }

    @Override
    public List<SnapshotPolicyVO> listActivePolicies() {
        SearchCriteria sc = ActivePolicySearch.create();
        sc.setParameters("active", true);
        return listBy(sc);
    }	
}