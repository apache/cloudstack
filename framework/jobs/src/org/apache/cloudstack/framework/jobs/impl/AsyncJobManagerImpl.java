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

package org.apache.cloudstack.framework.jobs.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.framework.config.ConfigDepot;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobExecutionContext;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.dao.AsyncJobDao;
import org.apache.cloudstack.framework.jobs.dao.AsyncJobJoinMapDao;
import org.apache.cloudstack.framework.jobs.dao.AsyncJobJournalDao;
import org.apache.cloudstack.framework.jobs.dao.SyncQueueItemDao;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.MessageDetector;
import org.apache.cloudstack.framework.messagebus.PublishScope;
import org.apache.cloudstack.jobs.JobInfo;
import org.apache.cloudstack.jobs.JobInfo.Status;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.log4j.Logger;

import com.cloud.cluster.ClusterManagerListener;
import com.cloud.cluster.ManagementServerHost;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Predicate;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.DbProperties;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExceptionUtil;
import com.cloud.utils.mgmt.JmxUtil;

public class AsyncJobManagerImpl extends ManagerBase implements AsyncJobManager, ClusterManagerListener, Configurable {
    // Advanced
    private static final ConfigKey<Long> JobExpireMinutes = new ConfigKey<Long>(Long.class, "job.expire.minutes", "Advanced", "1440",
        "Time (in minutes) for async-jobs to be kept in system", true, ConfigKey.Scope.Global, 60l);
    private static final ConfigKey<Long> JobCancelThresholdMinutes = new ConfigKey<Long>(Long.class, "job.cancel.threshold.minutes", "Advanced", "60",
        "Time (in minutes) for async-jobs to be forcely cancelled if it has been in process for long", true, ConfigKey.Scope.Global, 60l);

    private static final Logger s_logger = Logger.getLogger(AsyncJobManagerImpl.class);

    private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 3; 	// 3 seconds
    private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC = 60; 	// 60 seconds

    private static final int MAX_ONETIME_SCHEDULE_SIZE = 50;
    private static final int HEARTBEAT_INTERVAL = 2000;
    private static final int GC_INTERVAL = 10000;				// 10 seconds

    @Inject
    private SyncQueueItemDao _queueItemDao;
    @Inject
    private SyncQueueManager _queueMgr;
    @Inject
    private AsyncJobDao _jobDao;
    @Inject
    private AsyncJobJournalDao _journalDao;
    @Inject
    private AsyncJobJoinMapDao _joinMapDao;
    @Inject
    private List<AsyncJobDispatcher> _jobDispatchers;
    @Inject
    private MessageBus _messageBus;
    @Inject
    private AsyncJobMonitor _jobMonitor;
    @Inject
    private ConfigDepot _configDepot;

    private volatile long _executionRunNumber = 1;

    private final ScheduledExecutorService _heartbeatScheduler = Executors.newScheduledThreadPool(1, new NamedThreadFactory("AsyncJobMgr-Heartbeat"));
    private ExecutorService _executor;

    @Override
    public String getConfigComponentName() {
        return AsyncJobManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {JobExpireMinutes, JobCancelThresholdMinutes};
    }

    @Override
    public AsyncJobVO getAsyncJob(long jobId) {
        return _jobDao.findById(jobId);
    }

    @Override
    public List<AsyncJobVO> findInstancePendingAsyncJobs(String instanceType, Long accountId) {
        return _jobDao.findInstancePendingAsyncJobs(instanceType, accountId);
    }

    @Override
    @DB
    public AsyncJob getPseudoJob(long accountId, long userId) {
        AsyncJobVO job = _jobDao.findPseudoJob(Thread.currentThread().getId(), getMsid());
        if (job == null) {
            job = new AsyncJobVO();
            job.setAccountId(accountId);
            job.setUserId(userId);
            job.setInitMsid(getMsid());
            job.setDispatcher(AsyncJobVO.JOB_DISPATCHER_PSEUDO);
            job.setInstanceType(AsyncJobVO.PSEUDO_JOB_INSTANCE_TYPE);
            job.setInstanceId(Thread.currentThread().getId());
            _jobDao.persist(job);
        }
        return job;
    }

    @Override
    public long submitAsyncJob(AsyncJob job) {
        return submitAsyncJob(job, false);
    }

    @SuppressWarnings("unchecked")
    @DB
    public long submitAsyncJob(AsyncJob job, boolean scheduleJobExecutionInContext) {
        @SuppressWarnings("rawtypes")
        GenericDao dao = GenericDaoBase.getDao(job.getClass());
        job.setInitMsid(getMsid());
        job.setSyncSource(null);        // no sync source originally
        dao.persist(job);

        scheduleExecution(job, scheduleJobExecutionInContext);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("submit async job-" + job.getId() + ", details: " + job.toString());
        }
        return job.getId();
    }

    @SuppressWarnings("unchecked")
    @Override
    @DB
    public long submitAsyncJob(final AsyncJob job, final String syncObjType, final long syncObjId) {
        try {
            @SuppressWarnings("rawtypes")
            final GenericDao dao = GenericDaoBase.getDao(job.getClass());

            return Transaction.execute(new TransactionCallback<Long>() {
                @Override
                public Long doInTransaction(TransactionStatus status) {
                    job.setInitMsid(getMsid());
                    dao.persist(job);

                    syncAsyncJobExecution(job, syncObjType, syncObjId, 1);

                    return job.getId();
                }
            });
        } catch (Exception e) {
            String errMsg = "Unable to schedule async job for command " + job.getCmd() + ", unexpected exception.";
            s_logger.warn(errMsg, e);
            throw new CloudRuntimeException(errMsg);
        }
    }

    @Override
    @DB
    public void completeAsyncJob(final long jobId, final Status jobStatus, final int resultCode, final String resultObject) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Complete async job-" + jobId + ", jobStatus: " + jobStatus + ", resultCode: " + resultCode + ", result: " + resultObject);
        }

        final AsyncJobVO job = _jobDao.findById(jobId);
        if (job == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("job-" + jobId + " no longer exists, we just log completion info here. " + jobStatus + ", resultCode: " + resultCode + ", result: " +
                               resultObject);
            }

            return;
        }

        if (job.getStatus() != JobInfo.Status.IN_PROGRESS) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("job-" + jobId + " is already completed.");
            }

            return;
        }

        List<Long> wakeupList = Transaction.execute(new TransactionCallback<List<Long>>() {
            @Override
            public List<Long> doInTransaction(TransactionStatus status) {
                job.setCompleteMsid(getMsid());
                job.setStatus(jobStatus);
                job.setResultCode(resultCode);

                // reset attached object
                job.setInstanceType(null);
                job.setInstanceId(null);

                if (resultObject != null) {
                    job.setResult(resultObject);
                }

                job.setLastUpdated(DateUtil.currentGMTTime());
                _jobDao.update(jobId, job);

                List<Long> wakeupList = wakeupByJoinedJobCompletion(jobId);
                _joinMapDao.disjoinAllJobs(jobId);

                return wakeupList;
            }
        });

        for (Long id : wakeupList) {
            // TODO, we assume that all jobs in this category is API job only
            AsyncJobVO jobToWakeup = _jobDao.findById(id);
            if (jobToWakeup != null && (jobToWakeup.getPendingSignals() & AsyncJob.Constants.SIGNAL_MASK_WAKEUP) != 0)
                scheduleExecution(jobToWakeup, false);
        }

        _messageBus.publish(null, AsyncJob.Topics.JOB_STATE, PublishScope.GLOBAL, jobId);
    }

    @Override
    @DB
    public void updateAsyncJobStatus(final long jobId, final int processStatus, final String resultObject) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Update async-job progress, job-" + jobId + ", processStatus: " + processStatus + ", result: " + resultObject);
        }

        final AsyncJobVO job = _jobDao.findById(jobId);
        if (job == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("job-" + jobId + " no longer exists, we just log progress info here. progress status: " + processStatus);
            }

            return;
        }

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                job.setProcessStatus(processStatus);
                if (resultObject != null) {
                    job.setResult(resultObject);
                }
                job.setLastUpdated(DateUtil.currentGMTTime());
                _jobDao.update(jobId, job);
            }
        });
    }

    @Override
    @DB
    public void updateAsyncJobAttachment(final long jobId, final String instanceType, final Long instanceId) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Update async-job attachment, job-" + jobId + ", instanceType: " + instanceType + ", instanceId: " + instanceId);
        }

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                AsyncJobVO job = _jobDao.createForUpdate();
                job.setInstanceType(instanceType);
                job.setInstanceId(instanceId);
                job.setLastUpdated(DateUtil.currentGMTTime());
                _jobDao.update(jobId, job);
            }
        });
    }

    @Override
    @DB
    public void logJobJournal(long jobId, AsyncJob.JournalType journalType, String journalText, String journalObjJson) {
        AsyncJobJournalVO journal = new AsyncJobJournalVO();
        journal.setJobId(jobId);
        journal.setJournalType(journalType);
        journal.setJournalText(journalText);
        journal.setJournalObjJsonString(journalObjJson);

        _journalDao.persist(journal);
    }

    @Override
    @DB
    public void joinJob(long jobId, long joinJobId) {
        _joinMapDao.joinJob(jobId, joinJobId, getMsid(), 0, 0, null, null, null);
    }

    @Override
    @DB
    public void joinJob(long jobId, long joinJobId, String wakeupHandler, String wakeupDispatcher, String[] wakeupTopcisOnMessageBus, long wakeupIntervalInMilliSeconds,
        long timeoutInMilliSeconds) {

        Long syncSourceId = null;
        AsyncJobExecutionContext context = AsyncJobExecutionContext.getCurrentExecutionContext();
        assert (context.getJob() != null);
        if (context.getJob().getSyncSource() != null) {
            syncSourceId = context.getJob().getSyncSource().getQueueId();
        }

        _joinMapDao.joinJob(jobId, joinJobId, getMsid(), wakeupIntervalInMilliSeconds, timeoutInMilliSeconds, syncSourceId, wakeupHandler, wakeupDispatcher);
    }

    @Override
    @DB
    public void disjoinJob(long jobId, long joinedJobId) {
        _joinMapDao.disjoinJob(jobId, joinedJobId);
    }

    @Override
    @DB
    public void completeJoin(long joinJobId, JobInfo.Status joinStatus, String joinResult) {
        //
        // TODO
        // this is a temporary solution to solve strange MySQL deadlock issue,
        // completeJoin() causes deadlock happens at async_job table
        // I removed the temporary solution already.  I think my changes should fix the deadlock.

        /*
            	------------------------
            	LATEST DETECTED DEADLOCK
            	------------------------
            	130625 20:03:10
            	*** (1) TRANSACTION:
            	TRANSACTION 0 98087127, ACTIVE 0 sec, process no 1489, OS thread id 139837829175040 fetching rows, thread declared inside InnoDB 494
            	mysql tables in use 2, locked 1
            	LOCK WAIT 3 lock struct(s), heap size 368, 2 row lock(s), undo log entries 1
            	MySQL thread id 28408, query id 368571321 localhost 127.0.0.1 cloud preparing
            	UPDATE async_job SET job_pending_signals=1 WHERE id IN (SELECT job_id FROM async_job_join_map WHERE join_job_id = 9)
            	*** (1) WAITING FOR THIS LOCK TO BE GRANTED:
            	RECORD LOCKS space id 0 page no 1275 n bits 80 index `PRIMARY` of table `cloud`.`async_job` trx id 0 98087127 lock_mode X locks rec but not gap waiting
            	Record lock, heap no 9 PHYSICAL RECORD: n_fields 26; compact format; info bits 0
            	0: len 8; hex 0000000000000008; asc         ;; 1: len 6; hex 000005d8b0d8; asc       ;; 2: len 7; hex 00000009270110; asc     '  ;; 3: len 8; hex 0000000000000002; asc         ;; 4: len 8; hex 0000000000000002; asc         ;; 5: SQL NULL; 6: SQL NULL; 7: len 30; hex 6f72672e6170616368652e636c6f7564737461636b2e6170692e636f6d6d; asc org.apache.cloudstack.api.comm;...(truncated); 8: len 30; hex 7b226964223a2232222c22706879736963616c6e6574776f726b6964223a; asc {"id":"2","physicalnetworkid":;...(truncated); 9: len 4; hex 80000000; asc     ;; 10: len 4; hex 80000001; asc     ;; 11: len 4; hex 80000000; asc     ;; 12: len 4; hex 80000000; asc     ;; 13: len 30; hex 6f72672e6170616368652e636c6f7564737461636b2e6170692e72657370; asc org.apache.cloudstack.api.resp;...(truncated); 14: len 8; hex 80001a6f7bb0d0a8; asc    o{   ;; 15: len 8; hex 80001a6f7bb0d0a8; asc    o{   ;; 16: len 8; hex 8000124f06cfd5b6; asc    O    ;; 17: len 8; hex 8000124f06cfd5b6; asc    O    ;; 18: SQL NULL; 19: SQL NULL; 20: len 30; hex 66376466396532362d323139622d346338652d393231332d393766653636; asc f7df9e26-219b-4c8e-9213-97fe66;...(truncated); 21: len 30; hex 36623238306364362d663436652d343563322d383833642d333863616439; asc 6b280cd6-f46e-45c2-883d-38cad9;...(truncated); 22: SQL NULL; 23: len 21; hex 4170694173796e634a6f6244697370617463686572; asc ApiAsyncJobDispatcher;; 24: SQL NULL; 25: len 4; hex 80000000; asc     ;;

            	*** (2) TRANSACTION:
            	TRANSACTION 0 98087128, ACTIVE 0 sec, process no 1489, OS thread id 139837671909120 fetching rows, thread declared inside InnoDB 492
            	mysql tables in use 2, locked 1
            	3 lock struct(s), heap size 368, 2 row lock(s), undo log entries 1
            	MySQL thread id 28406, query id 368571323 localhost 127.0.0.1 cloud preparing
            	UPDATE async_job SET job_pending_signals=1 WHERE id IN (SELECT job_id FROM async_job_join_map WHERE join_job_id = 8)
            	*** (2) HOLDS THE LOCK(S):
            	RECORD LOCKS space id 0 page no 1275 n bits 80 index `PRIMARY` of table `cloud`.`async_job` trx id 0 98087128 lock_mode X locks rec but not gap
            	Record lock, heap no 9 PHYSICAL RECORD: n_fields 26; compact format; info bits 0
            	0: len 8; hex 0000000000000008; asc         ;; 1: len 6; hex 000005d8b0d8; asc       ;; 2: len 7; hex 00000009270110; asc     '  ;; 3: len 8; hex 0000000000000002; asc         ;; 4: len 8; hex 0000000000000002; asc         ;; 5: SQL NULL; 6: SQL NULL; 7: len 30; hex 6f72672e6170616368652e636c6f7564737461636b2e6170692e636f6d6d; asc org.apache.cloudstack.api.comm;...(truncated); 8: len 30; hex 7b226964223a2232222c22706879736963616c6e6574776f726b6964223a; asc {"id":"2","physicalnetworkid":;...(truncated); 9: len 4; hex 80000000; asc     ;; 10: len 4; hex 80000001; asc     ;; 11: len 4; hex 80000000; asc     ;; 12: len 4; hex 80000000; asc     ;; 13: len 30; hex 6f72672e6170616368652e636c6f7564737461636b2e6170692e72657370; asc org.apache.cloudstack.api.resp;...(truncated); 14: len 8; hex 80001a6f7bb0d0a8; asc    o{   ;; 15: len 8; hex 80001a6f7bb0d0a8; asc    o{   ;; 16: len 8; hex 8000124f06cfd5b6; asc    O    ;; 17: len 8; hex 8000124f06cfd5b6; asc    O    ;; 18: SQL NULL; 19: SQL NULL; 20: len 30; hex 66376466396532362d323139622d346338652d393231332d393766653636; asc f7df9e26-219b-4c8e-9213-97fe66;...(truncated); 21: len 30; hex 36623238306364362d663436652d343563322d383833642d333863616439; asc 6b280cd6-f46e-45c2-883d-38cad9;...(truncated); 22: SQL NULL; 23: len 21; hex 4170694173796e634a6f6244697370617463686572; asc ApiAsyncJobDispatcher;; 24: SQL NULL; 25: len 4; hex 80000000; asc     ;;

            	*** (2) WAITING FOR THIS LOCK TO BE GRANTED:
            	RECORD LOCKS space id 0 page no 1275 n bits 80 index `PRIMARY` of table `cloud`.`async_job` trx id 0 98087128 lock_mode X locks rec but not gap waiting
            	Record lock, heap no 10 PHYSICAL RECORD: n_fields 26; compact format; info bits 0
            	0: len 8; hex 0000000000000009; asc         ;; 1: len 6; hex 000005d8b0d7; asc       ;; 2: len 7; hex 00000009280110; asc     (  ;; 3: len 8; hex 0000000000000002; asc         ;; 4: len 8; hex 0000000000000002; asc         ;; 5: SQL NULL; 6: SQL NULL; 7: len 30; hex 6f72672e6170616368652e636c6f7564737461636b2e6170692e636f6d6d; asc org.apache.cloudstack.api.comm;...(truncated); 8: len 30; hex 7b226964223a2233222c22706879736963616c6e6574776f726b6964223a; asc {"id":"3","physicalnetworkid":;...(truncated); 9: len 4; hex 80000000; asc     ;; 10: len 4; hex 80000001; asc     ;; 11: len 4; hex 80000000; asc     ;; 12: len 4; hex 80000000; asc     ;; 13: len 30; hex 6f72672e6170616368652e636c6f7564737461636b2e6170692e72657370; asc org.apache.cloudstack.api.resp;...(truncated); 14: len 8; hex 80001a6f7bb0d0a8; asc    o{   ;; 15: len 8; hex 80001a6f7bb0d0a8; asc    o{   ;; 16: len 8; hex 8000124f06cfd5b6; asc    O    ;; 17: len 8; hex 8000124f06cfd5b6; asc    O    ;; 18: SQL NULL; 19: SQL NULL; 20: len 30; hex 62313065306432342d336233352d343663622d386361622d623933623562; asc b10e0d24-3b35-46cb-8cab-b93b5b;...(truncated); 21: len 30; hex 39353664383563632d383336622d346663612d623738622d646238343739; asc 956d85cc-836b-4fca-b78b-db8479;...(truncated); 22: SQL NULL; 23: len 21; hex 4170694173796e634a6f6244697370617463686572; asc ApiAsyncJobDispatcher;; 24: SQL NULL; 25: len 4; hex 80000000; asc     ;;

            	*** WE ROLL BACK TRANSACTION (2)
        */

        _joinMapDao.completeJoin(joinJobId, joinStatus, joinResult, getMsid());
    }

    @Override
    public void syncAsyncJobExecution(AsyncJob job, String syncObjType, long syncObjId, long queueSizeLimit) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Sync job-" + job.getId() + " execution on object " + syncObjType + "." + syncObjId);
        }

        SyncQueueVO queue = null;

        // to deal with temporary DB exceptions like DB deadlock/Lock-wait time out cased rollbacks
        // we retry five times until we throw an exception
        Random random = new Random();

        for (int i = 0; i < 5; i++) {
            queue = _queueMgr.queue(syncObjType, syncObjId, SyncQueueItem.AsyncJobContentType, job.getId(), queueSizeLimit);
            if (queue != null) {
                break;
            }

            try {
                Thread.sleep(1000 + random.nextInt(5000));
            } catch (InterruptedException e) {
            }
        }

        if (queue == null)
            throw new CloudRuntimeException("Unable to insert queue item into database, DB is full?");
    }

    @Override
    public AsyncJob queryJob(long jobId, boolean updatePollTime) {
        AsyncJobVO job = _jobDao.findById(jobId);

        if (updatePollTime) {
            job.setLastPolled(DateUtil.currentGMTTime());
            _jobDao.update(jobId, job);
        }
        return job;
    }

    private void scheduleExecution(final AsyncJobVO job) {
        scheduleExecution(job, false);
    }

    private void scheduleExecution(final AsyncJob job, boolean executeInContext) {
        Runnable runnable = getExecutorRunnable(job);
        if (executeInContext) {
            runnable.run();
        } else {
            _executor.submit(runnable);
        }
    }

    private AsyncJobDispatcher getDispatcher(String dispatcherName) {
        assert (dispatcherName != null && !dispatcherName.isEmpty()) : "Who's not setting the dispatcher when submitting a job?  Who am I suppose to call if you do that!";

        for (AsyncJobDispatcher dispatcher : _jobDispatchers) {
            if (dispatcherName.equals(dispatcher.getName()))
                return dispatcher;
        }

        throw new CloudRuntimeException("Unable to find dispatcher name: " + dispatcherName);
    }

    private AsyncJobDispatcher getWakeupDispatcher(AsyncJob job) {
        if (_jobDispatchers != null) {
            List<AsyncJobJoinMapVO> joinRecords = _joinMapDao.listJoinRecords(job.getId());
            if (joinRecords.size() > 0) {
                AsyncJobJoinMapVO joinRecord = joinRecords.get(0);
                for (AsyncJobDispatcher dispatcher : _jobDispatchers) {
                    if (dispatcher.getName().equals(joinRecord.getWakeupDispatcher()))
                        return dispatcher;
                }
            } else {
                s_logger.warn("job-" + job.getId() + " is scheduled for wakeup run, but there is no joining info anymore");
            }
        }
        return null;
    }

    private long getJobRunNumber() {
        synchronized (this) {
            return _executionRunNumber++;
        }
    }

    private Runnable getExecutorRunnable(final AsyncJob job) {
        return new ManagedContextRunnable() {
            @Override
            protected void runInContext() {
                long runNumber = getJobRunNumber();

                try {
                    //
                    // setup execution environment
                    //
                    try {
                        JmxUtil.registerMBean("AsyncJobManager", "Active Job " + job.getId(), new AsyncJobMBeanImpl(job));
                    } catch (Exception e) {
                        // Due to co-existence of normal-dispatched-job/wakeup-dispatched-job, MBean register() call
                        // is expected to fail under situations
                        if (s_logger.isTraceEnabled())
                            s_logger.trace("Unable to register active job " + job.getId() + " to JMX monitoring due to exception " + ExceptionUtil.toString(e));
                    }

                    _jobMonitor.registerActiveTask(runNumber, job.getId());
                    AsyncJobExecutionContext.setCurrentExecutionContext(new AsyncJobExecutionContext(job));

                    // execute the job
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Executing " + job);
                    }

                    if ((getAndResetPendingSignals(job) & AsyncJob.Constants.SIGNAL_MASK_WAKEUP) != 0) {
                        AsyncJobDispatcher jobDispatcher = getWakeupDispatcher(job);
                        if (jobDispatcher != null) {
                            jobDispatcher.runJob(job);
                        } else {
                            s_logger.error("Unable to find a wakeup dispatcher from the joined job: " + job);
                        }
                    } else {
                        AsyncJobDispatcher jobDispatcher = getDispatcher(job.getDispatcher());
                        if (jobDispatcher != null) {
                            jobDispatcher.runJob(job);
                        } else {
                            s_logger.error("Unable to find job dispatcher, job will be cancelled");
                            completeAsyncJob(job.getId(), JobInfo.Status.FAILED, ApiErrorCode.INTERNAL_ERROR.getHttpCode(), null);
                        }
                    }

                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Done executing " + job.getCmd() + " for job-" + job.getId());
                    }

                } catch (Throwable e) {
                    s_logger.error("Unexpected exception", e);
                    completeAsyncJob(job.getId(), JobInfo.Status.FAILED, ApiErrorCode.INTERNAL_ERROR.getHttpCode(), null);
                } finally {
                    // guard final clause as well
                    try {
                        AsyncJobVO jobToUpdate = _jobDao.findById(job.getId());
                        jobToUpdate.setExecutingMsid(null);
                        _jobDao.update(job.getId(), jobToUpdate);

                        if (job.getSyncSource() != null) {
                            _queueMgr.purgeItem(job.getSyncSource().getId());
                            checkQueue(job.getSyncSource().getQueueId());
                        }

                        try {
                            JmxUtil.unregisterMBean("AsyncJobManager", "Active Job " + job.getId());
                        } catch (Exception e) {
                            // Due to co-existence of normal-dispatched-job/wakeup-dispatched-job, MBean unregister() call
                            // is expected to fail under situations
                            if (s_logger.isTraceEnabled())
                                s_logger.trace("Unable to unregister job " + job.getId() + " to JMX monitoring due to exception " + ExceptionUtil.toString(e));
                        }

                        //
                        // clean execution environment
                        //
                        AsyncJobExecutionContext.unregister();
                        _jobMonitor.unregisterActiveTask(runNumber);

                    } catch (Throwable e) {
                        s_logger.error("Double exception", e);
                    }
                }
            }
        };
    }

    private int getAndResetPendingSignals(AsyncJob job) {
        int signals = job.getPendingSignals();
        if (signals != 0) {
            AsyncJobVO jobRecord = _jobDao.findById(job.getId());
            jobRecord.setPendingSignals(0);
            _jobDao.update(job.getId(), jobRecord);
        }
        return signals;
    }

    private void executeQueueItem(SyncQueueItemVO item, boolean fromPreviousSession) {
        AsyncJobVO job = _jobDao.findById(item.getContentId());
        if (job != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Schedule queued job-" + job.getId());
            }

            job.setSyncSource(item);

            job.setExecutingMsid(getMsid());
            _jobDao.update(job.getId(), job);

            try {
                scheduleExecution(job);
            } catch (RejectedExecutionException e) {
                s_logger.warn("Execution for job-" + job.getId() + " is rejected, return it to the queue for next turn");
                _queueMgr.returnItem(item.getId());

                job.setExecutingMsid(null);
                _jobDao.update(job.getId(), job);
            }

        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find related job for queue item: " + item.toString());
            }

            _queueMgr.purgeItem(item.getId());
        }
    }

    @Override
    public void releaseSyncSource() {
        AsyncJobExecutionContext executionContext = AsyncJobExecutionContext.getCurrentExecutionContext();
        assert (executionContext != null);

        if (executionContext.getSyncSource() != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Release sync source for job-" + executionContext.getJob().getId() + " sync source: " + executionContext.getSyncSource().getContentType() + "-" +
                               executionContext.getSyncSource().getContentId());
            }

            _queueMgr.purgeItem(executionContext.getSyncSource().getId());
            checkQueue(executionContext.getSyncSource().getQueueId());
        }
    }

    @Override
    public boolean waitAndCheck(AsyncJob job, String[] wakeupTopicsOnMessageBus, long checkIntervalInMilliSeconds, long timeoutInMiliseconds, Predicate predicate) {

        MessageDetector msgDetector = new MessageDetector();
        String[] topics = Arrays.copyOf(wakeupTopicsOnMessageBus, wakeupTopicsOnMessageBus.length + 1);
        topics[topics.length - 1] = AsyncJob.Topics.JOB_STATE;

        msgDetector.open(_messageBus, topics);
        try {
            long startTick = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTick < timeoutInMiliseconds) {
                msgDetector.waitAny(checkIntervalInMilliSeconds);
                job = _jobDao.findById(job.getId());
                if (job.getStatus().done()) {
                    return true;
                }

                if (predicate.checkCondition()) {
                    return true;
                }
            }
        } finally {
            msgDetector.close();
        }

        return false;
    }

    private void checkQueue(long queueId) {
        while (true) {
            try {
                SyncQueueItemVO item = _queueMgr.dequeueFromOne(queueId, getMsid());
                if (item != null) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Executing sync queue item: " + item.toString());
                    }

                    executeQueueItem(item, false);
                } else {
                    break;
                }
            } catch (Throwable e) {
                s_logger.error("Unexpected exception when kicking sync queue-" + queueId, e);
                break;
            }
        }
    }

    private Runnable getHeartbeatTask() {
        return new ManagedContextRunnable() {
            @Override
            protected void runInContext() {
                try {
                    List<SyncQueueItemVO> l = _queueMgr.dequeueFromAny(getMsid(), MAX_ONETIME_SCHEDULE_SIZE);
                    if (l != null && l.size() > 0) {
                        for (SyncQueueItemVO item : l) {
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Execute sync-queue item: " + item.toString());
                            }
                            executeQueueItem(item, false);
                        }
                    }

                    List<Long> standaloneWakeupJobs = wakeupScan();
                    for (Long jobId : standaloneWakeupJobs) {
                        // TODO, we assume that all jobs in this category is API job only
                        AsyncJobVO job = _jobDao.findById(jobId);
                        if (job != null && (job.getPendingSignals() & AsyncJob.Constants.SIGNAL_MASK_WAKEUP) != 0)
                            scheduleExecution(job, false);
                    }
                } catch (Throwable e) {
                    s_logger.error("Unexpected exception when trying to execute queue item, ", e);
                }
            }
        };
    }

    @DB
    private Runnable getGCTask() {
        return new ManagedContextRunnable() {
            @Override
            protected void runInContext() {
                GlobalLock scanLock = GlobalLock.getInternLock("AsyncJobManagerGC");
                try {
                    if (scanLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
                        try {
                            reallyRun();
                        } finally {
                            scanLock.unlock();
                        }
                    }
                } finally {
                    scanLock.releaseRef();
                }
            }

            public void reallyRun() {
                try {
                    s_logger.trace("Begin cleanup expired async-jobs");

                    Date cutTime = new Date(DateUtil.currentGMTTime().getTime() - JobExpireMinutes.value() * 1000);

                    // limit to 100 jobs per turn, this gives cleanup throughput as 600 jobs per minute
                    // hopefully this will be fast enough to balance potential growth of job table
                    //1) Expire unfinished jobs that weren't processed yet
                    List<AsyncJobVO> l = _jobDao.getExpiredUnfinishedJobs(cutTime, 100);
                    for (AsyncJobVO job : l) {
                        s_logger.trace("Expunging unfinished job " + job);
                        expungeAsyncJob(job);
                    }

                    //2) Expunge finished jobs
                    List<AsyncJobVO> completedJobs = _jobDao.getExpiredCompletedJobs(cutTime, 100);
                    for (AsyncJobVO job : completedJobs) {
                        s_logger.trace("Expunging completed job " + job);
                        expungeAsyncJob(job);
                    }

                    // forcefully cancel blocking queue items if they've been staying there for too long
                    List<SyncQueueItemVO> blockItems = _queueMgr.getBlockedQueueItems(JobCancelThresholdMinutes.value() * 1000, false);
                    if (blockItems != null && blockItems.size() > 0) {
                        for (SyncQueueItemVO item : blockItems) {
                            if (item.getContentType().equalsIgnoreCase(SyncQueueItem.AsyncJobContentType)) {
                                completeAsyncJob(item.getContentId(), JobInfo.Status.FAILED, 0, "Job is cancelled as it has been blocking others for too long");
                            }

                            // purge the item and resume queue processing
                            _queueMgr.purgeItem(item.getId());
                        }
                    }

                    s_logger.trace("End cleanup expired async-jobs");
                } catch (Throwable e) {
                    s_logger.error("Unexpected exception when trying to execute queue item, ", e);
                }
            }
        };
    }

    @DB
    protected void expungeAsyncJob(final AsyncJobVO job) {
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                _jobDao.expunge(job.getId());
                //purge corresponding sync queue item
                _queueMgr.purgeAsyncJobQueueItemId(job.getId());
            }
        });
    }

    private long getMsid() {
        return ManagementServerNode.getManagementServerId();
    }

    private void cleanupPendingJobs(List<SyncQueueItemVO> l) {
        for (SyncQueueItemVO item : l) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Discard left-over queue item: " + item.toString());
            }

            String contentType = item.getContentType();
            if (contentType != null && contentType.equalsIgnoreCase(SyncQueueItem.AsyncJobContentType)) {
                Long jobId = item.getContentId();
                if (jobId != null) {
                    s_logger.warn("Mark job as failed as its correspoding queue-item has been discarded. job id: " + jobId);
                    completeAsyncJob(jobId, JobInfo.Status.FAILED, 0, "Execution was cancelled because of server shutdown");
                }
            }
            _queueMgr.purgeItem(item.getId());
        }
    }

    @DB
    protected List<Long> wakeupByJoinedJobCompletion(long joinedJobId) {
        SearchCriteria<Long> joinJobSC = JoinJobSearch.create("joinJobId", joinedJobId);

        List<Long> result = _joinMapDao.customSearch(joinJobSC, null);
        if (result.size() > 0) {
            Collections.sort(result);
            Long[] ids = result.toArray(new Long[result.size()]);

            final SearchCriteria<AsyncJobVO> jobsSC = JobIdsSearch.create("ids", ids);
            final SearchCriteria<SyncQueueItemVO> queueItemsSC = QueueJobIdsSearch.create("contentIds", ids);

            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    AsyncJobVO job = _jobDao.createForUpdate();
                    job.setPendingSignals(AsyncJob.Constants.SIGNAL_MASK_WAKEUP);
                    _jobDao.update(job, jobsSC);

                    SyncQueueItemVO item = _queueItemDao.createForUpdate();
                    item.setLastProcessNumber(null);
                    item.setLastProcessMsid(null);
                    _queueItemDao.update(item, queueItemsSC);
                }
            });
        }
        return _joinMapDao.findJobsToWake(joinedJobId);
    }

    @DB
    protected List<Long> wakeupScan() {
        final Date cutDate = DateUtil.currentGMTTime();

        SearchCriteria<Long> sc = JoinJobTimeSearch.create();
        sc.setParameters("beginTime", cutDate);
        sc.setParameters("endTime", cutDate);

        final List<Long> result = _joinMapDao.customSearch(sc, null);

        return Transaction.execute(new TransactionCallback<List<Long>>() {
            @Override
            public List<Long> doInTransaction(TransactionStatus status) {
                if (result.size() > 0) {
                    Collections.sort(result);
                    Long[] ids = result.toArray(new Long[result.size()]);

                    AsyncJobVO job = _jobDao.createForUpdate();
                    job.setPendingSignals(AsyncJob.Constants.SIGNAL_MASK_WAKEUP);

                    SearchCriteria<AsyncJobVO> sc2 = JobIdsSearch.create("ids", ids);
                    SearchCriteria<SyncQueueItemVO> queueItemsSC = QueueJobIdsSearch.create("contentIds", ids);

                    _jobDao.update(job, sc2);

                    SyncQueueItemVO item = _queueItemDao.createForUpdate();
                    item.setLastProcessNumber(null);
                    item.setLastProcessMsid(null);
                    _queueItemDao.update(item, queueItemsSC);
                }

                return _joinMapDao.findJobsToWakeBetween(cutDate);
            }
        });
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        try {
            final Properties dbProps = DbProperties.getDbProperties();
            final int cloudMaxActive = Integer.parseInt(dbProps.getProperty("db.cloud.maxActive"));

            int poolSize = (cloudMaxActive * 2) / 3;

            s_logger.info("Start AsyncJobManager thread pool in size " + poolSize);
            _executor = Executors.newFixedThreadPool(poolSize, new NamedThreadFactory(AsyncJobManager.JOB_POOL_THREAD_PREFIX));
        } catch (final Exception e) {
            throw new ConfigurationException("Unable to load db.properties to configure AsyncJobManagerImpl");
        }

        JoinJobSearch = _joinMapDao.createSearchBuilder(Long.class);
        JoinJobSearch.and(JoinJobSearch.entity().getJoinJobId(), Op.EQ, "joinJobId");
        JoinJobSearch.selectFields(JoinJobSearch.entity().getJobId());
        JoinJobSearch.done();

        JoinJobTimeSearch = _joinMapDao.createSearchBuilder(Long.class);
        JoinJobTimeSearch.and(JoinJobTimeSearch.entity().getNextWakeupTime(), Op.LT, "beginTime");
        JoinJobTimeSearch.and(JoinJobTimeSearch.entity().getExpiration(), Op.GT, "endTime");
        JoinJobTimeSearch.selectFields(JoinJobTimeSearch.entity().getJobId()).done();

        JobIdsSearch = _jobDao.createSearchBuilder();
        JobIdsSearch.and(JobIdsSearch.entity().getId(), Op.IN, "ids").done();

        QueueJobIdsSearch = _queueItemDao.createSearchBuilder();
        QueueJobIdsSearch.and(QueueJobIdsSearch.entity().getContentId(), Op.IN, "contentIds").done();

        JoinJobIdsSearch = _joinMapDao.createSearchBuilder(Long.class);
        JoinJobIdsSearch.selectFields(JoinJobIdsSearch.entity().getJobId());
        JoinJobIdsSearch.and(JoinJobIdsSearch.entity().getJoinJobId(), Op.EQ, "joinJobId");
        JoinJobIdsSearch.and(JoinJobIdsSearch.entity().getJobId(), Op.NIN, "jobIds");
        JoinJobIdsSearch.done();

        ContentIdsSearch = _queueItemDao.createSearchBuilder(Long.class);
        ContentIdsSearch.selectFields(ContentIdsSearch.entity().getContentId()).done();

        AsyncJobExecutionContext.init(this, _joinMapDao);
        OutcomeImpl.init(this);

        return true;
    }

    @Override
    public void onManagementNodeJoined(List<? extends ManagementServerHost> nodeList, long selfNodeId) {
    }

    @Override
    public void onManagementNodeLeft(List<? extends ManagementServerHost> nodeList, long selfNodeId) {
        for (final ManagementServerHost msHost : nodeList) {
            try {
                Transaction.execute(new TransactionCallbackNoReturn() {
                    @Override
                    public void doInTransactionWithoutResult(TransactionStatus status) {
                        List<SyncQueueItemVO> items = _queueMgr.getActiveQueueItems(msHost.getId(), true);
                        cleanupPendingJobs(items);
                        _jobDao.resetJobProcess(msHost.getId(), ApiErrorCode.INTERNAL_ERROR.getHttpCode(), "job cancelled because of management server restart");
                    }
                });
            } catch (Throwable e) {
                s_logger.warn("Unexpected exception ", e);
            }
        }
    }

    @Override
    public void onManagementNodeIsolated() {
    }

    @Override
    public boolean start() {
        try {
            _jobDao.cleanupPseduoJobs(getMsid());

            List<SyncQueueItemVO> l = _queueMgr.getActiveQueueItems(getMsid(), false);
            cleanupPendingJobs(l);
            _jobDao.resetJobProcess(getMsid(), ApiErrorCode.INTERNAL_ERROR.getHttpCode(), "job cancelled because of management server restart");
        } catch (Throwable e) {
            s_logger.error("Unexpected exception " + e.getMessage(), e);
        }

        _heartbeatScheduler.scheduleAtFixedRate(getHeartbeatTask(), HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
        _heartbeatScheduler.scheduleAtFixedRate(getGCTask(), GC_INTERVAL, GC_INTERVAL, TimeUnit.MILLISECONDS);

        return true;
    }

    @Override
    public boolean stop() {
        _heartbeatScheduler.shutdown();
        _executor.shutdown();
        return true;
    }

    private GenericSearchBuilder<SyncQueueItemVO, Long> ContentIdsSearch;
    private GenericSearchBuilder<AsyncJobJoinMapVO, Long> JoinJobSearch;
    private SearchBuilder<AsyncJobVO> JobIdsSearch;
    private SearchBuilder<SyncQueueItemVO> QueueJobIdsSearch;
    private GenericSearchBuilder<AsyncJobJoinMapVO, Long> JoinJobIdsSearch;
    private GenericSearchBuilder<AsyncJobJoinMapVO, Long> JoinJobTimeSearch;

    protected AsyncJobManagerImpl() {

    }

}
