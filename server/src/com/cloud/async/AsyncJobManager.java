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

package com.cloud.async;

import java.util.List;

import com.cloud.api.commands.QueryAsyncJobResultCmd;
import com.cloud.utils.component.Manager;

public interface AsyncJobManager extends Manager {
    
	public AsyncJobExecutorContext getExecutorContext();
	
	public AsyncJobVO getAsyncJob(long jobId);
	public AsyncJobVO findInstancePendingAsyncJob(String instanceType, long instanceId);	
	public List<? extends AsyncJob> findInstancePendingAsyncJobs(AsyncJob.Type instanceType, Long accountId);
	
	public long submitAsyncJob(AsyncJobVO job);
	public long submitAsyncJob(AsyncJobVO job, boolean scheduleJobExecutionInContext);
	public AsyncJobResult queryAsyncJobResult(long jobId);    
	
    public void completeAsyncJob(long jobId, int jobStatus, int resultCode, Object resultObject);
    public void updateAsyncJobStatus(long jobId, int processStatus, Object resultObject);
    public void updateAsyncJobAttachment(long jobId, String instanceType, Long instanceId);
   
    public void releaseSyncSource(AsyncJobExecutor executor);
    
    public void syncAsyncJobExecution(AsyncJob job, String syncObjType, long syncObjId);
    
    /**
     * Queries for the status or final result of an async job.
     * @param cmd the command that specifies the job id
     * @return an async-call result object
     */
    public AsyncJob queryAsyncJobResult(QueryAsyncJobResultCmd cmd);
}
