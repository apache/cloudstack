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

package com.cloud.async.dao;

import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.async.AsyncJobResult;
import com.cloud.async.AsyncJobVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value = { AsyncJobDao.class })
public class AsyncJobDaoImpl extends GenericDaoBase<AsyncJobVO, Long> implements AsyncJobDao {
    private static final Logger s_logger = Logger.getLogger(AsyncJobDaoImpl.class.getName());
	
	private SearchBuilder<AsyncJobVO> pendingAsyncJobSearch;		
	private SearchBuilder<AsyncJobVO> expiringAsyncJobSearch;		
	
	public AsyncJobDaoImpl() {
		pendingAsyncJobSearch = createSearchBuilder();
		pendingAsyncJobSearch.and("instanceType", pendingAsyncJobSearch.entity().getInstanceType(), 
			SearchCriteria.Op.EQ);
		pendingAsyncJobSearch.and("instanceId", pendingAsyncJobSearch.entity().getInstanceId(), 
			SearchCriteria.Op.EQ);
		pendingAsyncJobSearch.and("status", pendingAsyncJobSearch.entity().getStatus(), 
				SearchCriteria.Op.EQ);
		pendingAsyncJobSearch.done();
		
		expiringAsyncJobSearch = createSearchBuilder();
		expiringAsyncJobSearch.and("created", expiringAsyncJobSearch.entity().getCreated(), 
			SearchCriteria.Op.LTEQ);
		expiringAsyncJobSearch.done();
	}
	
	public AsyncJobVO findInstancePendingAsyncJob(String instanceType, long instanceId) {
        SearchCriteria sc = pendingAsyncJobSearch.create();
        sc.setParameters("instanceType", instanceType);
        sc.setParameters("instanceId", instanceId);
        sc.setParameters("status", AsyncJobResult.STATUS_IN_PROGRESS);
        
        List<AsyncJobVO> l = listBy(sc);
        if(l != null && l.size() > 0) {
        	if(l.size() > 1) {
        		s_logger.warn("Instance " + instanceType + "-" + instanceId + " has multiple pending async-job");
        	}
        	
        	return l.get(0);
        }
        return null;
	}
	
	public List<AsyncJobVO> getExpiredJobs(Date cutTime, int limit) {
		SearchCriteria sc = expiringAsyncJobSearch.create();
		sc.setParameters("created", cutTime);
		Filter filter = new Filter(AsyncJobVO.class, "created", true, 0L, (long)limit);
		return listBy(sc, filter);
	}
}
