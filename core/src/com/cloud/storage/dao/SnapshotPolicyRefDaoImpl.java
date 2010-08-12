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

import com.cloud.storage.SnapshotPolicyRefVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local (value={SnapshotPolicyRefDao.class})
public class SnapshotPolicyRefDaoImpl extends GenericDaoBase<SnapshotPolicyRefVO, Long> implements SnapshotPolicyRefDao {
	protected final SearchBuilder<SnapshotPolicyRefVO> snapPolicy;
	protected final SearchBuilder<SnapshotPolicyRefVO> snapSearch;
	protected final SearchBuilder<SnapshotPolicyRefVO> policySearch;
	
	protected SnapshotPolicyRefDaoImpl() {
		snapPolicy = createSearchBuilder();
		snapPolicy.and("snapshotId", snapPolicy.entity().getSnapshotId(), SearchCriteria.Op.EQ);
		snapPolicy.and("policyId", snapPolicy.entity().getPolicyId(), SearchCriteria.Op.EQ);
		snapPolicy.done();
		
		snapSearch = createSearchBuilder();
		snapSearch.and("snapshotId", snapSearch.entity().getSnapshotId(), SearchCriteria.Op.EQ);
		snapSearch.done();
		
		policySearch = createSearchBuilder();
		policySearch.and("policyId", policySearch.entity().getPolicyId(), SearchCriteria.Op.EQ);
		policySearch.and("volumeId", policySearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
		policySearch.done();
	}
	
	@Override
	public SnapshotPolicyRefVO findBySnapPolicy(long snapshotId, long policyId) {
		SearchCriteria sc = snapPolicy.create();
	    sc.setParameters("snapshotId", snapshotId);
	    sc.setParameters("policyId", policyId);
	    return findOneBy(sc);
	}
	
	@Override
	public int removeSnapPolicy(long snapshotId, long policyId) {
		SearchCriteria sc = snapPolicy.create();
	    sc.setParameters("snapshotId", snapshotId);
	    sc.setParameters("policyId", policyId);
	    return delete(sc);
	}

	@Override
	public List<SnapshotPolicyRefVO> listBySnapshotId(long snapshotId) {
	    SearchCriteria sc = snapSearch.create();
	    sc.setParameters("snapshotId", snapshotId);
	    return listBy(sc);
	}
	
	@Override
	public List<SnapshotPolicyRefVO> listByPolicyId(long policyId, long volumeId) {
	    SearchCriteria sc = policySearch.create();
	    sc.setParameters("policyId", policyId);
	    sc.setParameters("volumeId", volumeId);
	    return listBy(sc);
	}
}