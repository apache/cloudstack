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

/**
 * 
 */
package com.cloud.storage.dao;

import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import com.cloud.storage.SnapshotScheduleVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;


@Local (value={SnapshotScheduleDao.class})
public class SnapshotScheduleDaoImpl extends GenericDaoBase<SnapshotScheduleVO, Long> implements SnapshotScheduleDao {
	protected final SearchBuilder<SnapshotScheduleVO> executableSchedulesSearch;
	protected final SearchBuilder<SnapshotScheduleVO> coincidingSchedulesSearch;
	// DB constraint: For a given volume and policyId, there will only be one entry in this table.
	
	
	protected SnapshotScheduleDaoImpl() {
		
	    executableSchedulesSearch = createSearchBuilder();
        executableSchedulesSearch.and("scheduledTimestamp", executableSchedulesSearch.entity().getScheduledTimestamp(), SearchCriteria.Op.LT);
        executableSchedulesSearch.and("asyncJobId", executableSchedulesSearch.entity().getAsyncJobId(), SearchCriteria.Op.NULL);
        executableSchedulesSearch.done();
        
        coincidingSchedulesSearch = createSearchBuilder();
        coincidingSchedulesSearch.and("volumeId", coincidingSchedulesSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        coincidingSchedulesSearch.and("scheduledTimestamp", coincidingSchedulesSearch.entity().getScheduledTimestamp(), SearchCriteria.Op.LT);
        coincidingSchedulesSearch.and("asyncJobId", coincidingSchedulesSearch.entity().getAsyncJobId(), SearchCriteria.Op.NULL);
        coincidingSchedulesSearch.done();
		
	}
	
	/**
	 * {@inheritDoc} 
	 */
	@Override
	public List<SnapshotScheduleVO> getCoincidingSnapshotSchedules(long volumeId, Date date) {
		SearchCriteria sc = coincidingSchedulesSearch.create();
	    sc.setParameters("volumeId", volumeId);
	    sc.setParameters("scheduledTimestamp", date);
	    // Don't return manual snapshots. They will be executed through another code path.
        sc.addAnd("policyId", SearchCriteria.Op.NEQ, 1L);
        return listActiveBy(sc);
	}

	/**
     * {@inheritDoc} 
     */
    @Override
    public List<SnapshotScheduleVO> getSchedulesToExecute(Date currentTimestamp) {
        SearchCriteria sc = executableSchedulesSearch.create();
        sc.setParameters("scheduledTimestamp", currentTimestamp);
        // Don't return manual snapshots. They will be executed through another code path.
        sc.addAnd("policyId", SearchCriteria.Op.NEQ, 1L);
        return listActiveBy(sc);
    }
    
    /**
     * {@inheritDoc} 
     */
    @Override
    public SnapshotScheduleVO getCurrentSchedule(Long volumeId, Long policyId, boolean executing) {
        assert volumeId != null;
        SearchCriteria sc = createSearchCriteria();
        sc.addAnd("volumeId", SearchCriteria.Op.EQ, volumeId);
        if (policyId != null) {
            sc.addAnd("policyId", SearchCriteria.Op.EQ, policyId);
        }
        SearchCriteria.Op op = executing ? SearchCriteria.Op.NNULL : SearchCriteria.Op.NULL;
        sc.addAnd("asyncJobId", op);
        List<SnapshotScheduleVO> snapshotSchedules = listActiveBy(sc);
        // This will return only one schedule because of a DB uniqueness constraint.
        assert (snapshotSchedules.size() <= 1);
        if (snapshotSchedules.isEmpty()) {
            return null;
        }
        else {
            return snapshotSchedules.get(0);
        }
    }
    
}