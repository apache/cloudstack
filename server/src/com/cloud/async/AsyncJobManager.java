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
package com.cloud.async;

import java.util.List;

import org.apache.cloudstack.api.command.user.job.QueryAsyncJobResultCmd;

import com.cloud.utils.Predicate;
import com.cloud.utils.component.Manager;

public interface AsyncJobManager extends Manager {
    
	AsyncJobVO getAsyncJob(long jobId);
	
	List<? extends AsyncJob> findInstancePendingAsyncJobs(String instanceType, Long accountId);
	
	long submitAsyncJob(AsyncJob job);
	long submitAsyncJob(AsyncJob job, boolean scheduleJobExecutionInContext);
	long submitAsyncJob(AsyncJob job, String syncObjType, long syncObjId);
	AsyncJobResult queryAsyncJobResult(long jobId);

	/**
	 * A running thread inside management server can have a 1:1 linked pseudo job.
	 * This is to help make some legacy code work without too dramastic changes
	 *
	 * @return pseudo job for the thread
	 */
	AsyncJob getPseudoJob();
	
    void completeAsyncJob(long jobId, int jobStatus, int resultCode, Object resultObject);
    void updateAsyncJobStatus(long jobId, int processStatus, Object resultObject);
    void updateAsyncJobAttachment(long jobId, String instanceType, Long instanceId);
    void logJobJournal(long jobId, AsyncJob.JournalType journalType, String 
    	journalText, String journalObjJson);
    
    void joinJob(long jobId, long joinJobId);
    void disjoinJob(long jobId, long joinedJobId);
    void completeJoin(long joinJobId, int joinStatus, String joinResult);
   
    void releaseSyncSource();
    void syncAsyncJobExecution(AsyncJob job, String syncObjType, long syncObjId, long queueSizeLimit);
    
    boolean waitAndCheck(String[] wakupSubjects, long checkIntervalInMilliSeconds, 
    	long timeoutInMiliseconds, Predicate predicate);
    
    /**
     * Queries for the status or final result of an async job.
     * @param cmd the command that specifies the job id
     * @return an async-call result object
     */
    AsyncJob queryAsyncJobResult(QueryAsyncJobResultCmd cmd);
}
