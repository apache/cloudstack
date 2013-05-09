// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.async.dao;

import java.util.List;

import com.cloud.async.AsyncJobConstants;
import com.cloud.async.AsyncJobJoinMapVO;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.UpdateBuilder;
import com.cloud.utils.db.SearchCriteria.Op;

public class AsyncJobJoinMapDaoImpl extends GenericDaoBase<AsyncJobJoinMapVO, Long> implements AsyncJobJoinMapDao {
	
	private final SearchBuilder<AsyncJobJoinMapVO> RecordSearch;	
	private final SearchBuilder<AsyncJobJoinMapVO> RecordSearchByOwner;	
	private final SearchBuilder<AsyncJobJoinMapVO> CompleteJoinSearch;	
	
	public AsyncJobJoinMapDaoImpl() {
		RecordSearch = createSearchBuilder();
		RecordSearch.and("jobId", RecordSearch.entity().getJobId(), Op.EQ);
		RecordSearch.and("joinJobId", RecordSearch.entity().getJoinJobId(), Op.EQ);
		RecordSearch.done();

		RecordSearchByOwner = createSearchBuilder();
		RecordSearchByOwner.and("jobId", RecordSearchByOwner.entity().getJobId(), Op.EQ);
		RecordSearchByOwner.done();
		
		CompleteJoinSearch = createSearchBuilder();
		CompleteJoinSearch.and("joinJobId", CompleteJoinSearch.entity().getJoinJobId(), Op.EQ);
		CompleteJoinSearch.done();
	}
	
	public Long joinJob(long jobId, long joinJobId, long joinMsid, 
		Long syncSourceId, String wakeupHandler, String wakeupDispatcher) {
		
		AsyncJobJoinMapVO record = new AsyncJobJoinMapVO();
		record.setJobId(jobId);
		record.setJoinJobId(joinJobId);
		record.setJoinMsid(joinMsid);
		record.setJoinStatus(AsyncJobConstants.STATUS_IN_PROGRESS);
		record.setSyncSourceId(syncSourceId);
		record.setWakeupHandler(wakeupHandler);
		record.setWakeupHandler(wakeupHandler);
		
		this.persist(record);
		return record.getId();
	}
	
	public void disjoinJob(long jobId, long joinedJobId) {
		SearchCriteria<AsyncJobJoinMapVO> sc = RecordSearch.create();
		sc.setParameters("jobId", jobId);
		sc.setParameters("joinJobId", joinedJobId);
		
		this.expunge(sc);
	}
	
	public AsyncJobJoinMapVO getJoinRecord(long jobId, long joinJobId) {
		SearchCriteria<AsyncJobJoinMapVO> sc = RecordSearch.create();
		sc.setParameters("jobId", jobId);
		sc.setParameters("joinJobId", joinJobId);
		
		List<AsyncJobJoinMapVO> result = this.listBy(sc);
		if(result != null && result.size() > 0) {
			assert(result.size() == 1);
			return result.get(0);
		}
		
		return null;
	}
	
	public List<AsyncJobJoinMapVO> listJoinRecords(long jobId) {
		SearchCriteria<AsyncJobJoinMapVO> sc = RecordSearchByOwner.create();
		sc.setParameters("jobId", jobId);
		
		return this.listBy(sc);
	}
	
	public void completeJoin(long joinJobId, int joinStatus, String joinResult, long completeMsid) {
        AsyncJobJoinMapVO record = createForUpdate();
        record.setJoinStatus(joinStatus);
        record.setJoinResult(joinResult);
        record.setCompleteMsid(completeMsid);
        record.setLastUpdated(DateUtil.currentGMTTime());
        
        UpdateBuilder ub = getUpdateBuilder(record);
        
        SearchCriteria<AsyncJobJoinMapVO> sc = CompleteJoinSearch.create();
        sc.setParameters("joinJobId", joinJobId);
        update(ub, sc, null);
	}
}
