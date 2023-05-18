// Licensed to the Apache Software Foundation (ASF) under ones
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
package org.apache.cloudstack.framework.jobs;

import java.io.Serializable;
import java.util.List;

import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.jobs.JobInfo;

import com.cloud.utils.Predicate;
import com.cloud.utils.component.Manager;

public interface AsyncJobManager extends Manager {

    public static final String API_JOB_POOL_THREAD_PREFIX = "API-Job-Executor";
    public static final String WORK_JOB_POOL_THREAD_PREFIX = "Work-Job-Executor";

    AsyncJobVO getAsyncJob(long jobId);

    List<? extends AsyncJob> findInstancePendingAsyncJobs(String instanceType, Long accountId);

    long submitAsyncJob(AsyncJob job);

    long submitAsyncJob(AsyncJob job, String syncObjType, long syncObjId);

    void completeAsyncJob(long jobId, JobInfo.Status jobStatus, int resultCode, String result);

    void updateAsyncJobStatus(long jobId, int processStatus, String resultObject);

    void updateAsyncJobAttachment(long jobId, String instanceType, Long instanceId);
    void logJobJournal(long jobId, AsyncJob.JournalType journalType, String
            journalText, String journalObjJson);

    /**
     * A running thread inside management server can have a 1:1 linked pseudo job.
     * This is to help make some legacy code work without too dramatic changes.
     *
     * All pseudo jobs should be expunged upon management start event
     *
     * @return pseudo job for the thread
     */
    AsyncJob getPseudoJob(long accountId, long userId);

    /**
     * Used by upper level job to wait for completion of a down-level job (usually VmWork jobs)
     * in synchronous way. Caller needs to use waitAndCheck() to check the completion status
     * of the down-level job
     *
     * Due to the amount of legacy code that relies on synchronous-call semantics, this form of joinJob
     * is used mostly
     *
     *
     * @param jobId upper job that is going to wait the completion of a down-level job
     * @param joinJobId down-level job
     */
    void joinJob(long jobId, long joinJobId);

    /**
     * Used by upper level job to wait for completion of a down-level job (usually VmWork jobs)
     * in asynchronous way, it will cause upper job to cease current execution, upper job will be
     * rescheduled to execute periodically or on wakeup events detected from message bus
     *
     * @param jobId upper job that is going to wait the completion of a down-level job
     * @param joinJobId down-level job
     * @Param wakeupHandler    wake-up handler
     * @Param wakeupDispatcher wake-up dispatcher
     * @param wakeupTopicsOnMessageBus
     * @param wakeupIntervalInMilliSeconds
     * @param timeoutInMilliSeconds
     */
    void joinJob(long jobId, long joinJobId, String wakeupHandler, String wakupDispatcher,
            String[] wakeupTopicsOnMessageBus, long wakeupIntervalInMilliSeconds, long timeoutInMilliSeconds);

    /**
     * Dis-join two related jobs
     *
     * @param jobId
     * @param joinedJobId
     */
    void disjoinJob(long jobId, long joinedJobId);

    /**
     * Used by down-level job to notify its completion to upper level jobs
     *
     * @param joinJobId down-level job for upper level job to join with
     * @param joinStatus AsyncJobConstants status code to indicate success or failure of the
     *                     down-level job
     * @param joinResult object-stream serialized result object
     *                     this is primarily used by down-level job to pass error exception objects
     *                     for legacy code to work. To help pass exception object easier, we use
     *                     object-stream based serialization instead of GSON
     */
    void completeJoin(long joinJobId, JobInfo.Status joinStatus, String joinResult);

    void releaseSyncSource();

    void syncAsyncJobExecution(AsyncJob job, String syncObjType, long syncObjId, long queueSizeLimit);

    /**
     * This method will be deprecated after all code has been migrated to fully-asynchronous mode
     * that uses async-feature of joinJob/disjoinJob
     *
     * @param wakupTopicsOnMessageBus topic on message bus to wakeup the wait
     * @param checkIntervalInMilliSeconds time to break out wait for checking predicate condition
     * @param timeoutInMiliseconds time out to break out the whole wait process
     * @param predicate
     * @return true, predicate condition is satisfied
     *             false, wait is timed out
     */
    boolean waitAndCheck(AsyncJob job, String[] wakupTopicsOnMessageBus, long checkIntervalInMilliSeconds, long timeoutInMiliseconds, Predicate predicate);

    AsyncJob queryJob(long jobId, boolean updatePollTime);

    String marshallResultObject(Serializable obj);

    Object unmarshallResultObject(AsyncJob job);

    List<AsyncJobVO> findFailureAsyncJobs(String... cmds);

    long countPendingJobs(String havingInfo, String... cmds);

    // Returns the number of pending jobs for the given Management server msids.
    // NOTE: This is the msid and NOT the id
    long countPendingNonPseudoJobs(Long... msIds);

    void enableAsyncJobs();

    void disableAsyncJobs();

    boolean isAsyncJobsEnabled();
}
