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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.storage.dao.VolumeDetailsDao;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobExecutionContext;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.dao.AsyncJobDao;
import org.apache.cloudstack.framework.jobs.dao.VmWorkJobDao;
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
import org.apache.log4j.MDC;

import com.cloud.cluster.ClusterManagerListener;
import org.apache.cloudstack.management.ManagementServerHost;

import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.SnapshotDetailsVO;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Predicate;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.ComponentLifecycle;
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
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.storage.dao.VolumeDao;

import static com.cloud.utils.HumanReadableJson.getHumanReadableBytesJson;

public class AsyncJobManagerImpl extends ManagerBase implements AsyncJobManager, ClusterManagerListener, Configurable {
    // Advanced
    public static final ConfigKey<Long> JobExpireMinutes = new ConfigKey<Long>("Advanced", Long.class, "job.expire.minutes", "1440",
        "Time (in minutes) for async-jobs to be kept in system", true, ConfigKey.Scope.Global);
    public static final ConfigKey<Long> JobCancelThresholdMinutes = new ConfigKey<Long>("Advanced", Long.class, "job.cancel.threshold.minutes", "60",
        "Time (in minutes) for async-jobs to be forcely cancelled if it has been in process for long", true, ConfigKey.Scope.Global);
    private static final ConfigKey<Integer> VmJobLockTimeout = new ConfigKey<Integer>("Advanced",
            Integer.class, "vm.job.lock.timeout", "1800",
            "Time in seconds to wait in acquiring lock to submit a vm worker job", false);
    private static final ConfigKey<Boolean> HidePassword = new ConfigKey<Boolean>("Advanced", Boolean.class, "log.hide.password", "true", "If set to true, the password is hidden", true, ConfigKey.Scope.Global);

    private static final Logger s_logger = Logger.getLogger(AsyncJobManagerImpl.class);

    private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 3;     // 3 seconds

    private static final int MAX_ONETIME_SCHEDULE_SIZE = 50;
    private static final int HEARTBEAT_INTERVAL = 2000;
    private static final int GC_INTERVAL = 10000;                // 10 seconds

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
    private VMInstanceDao _vmInstanceDao;
    @Inject
    private VmWorkJobDao _vmWorkJobDao;
    @Inject
    private VolumeDetailsDao _volumeDetailsDao;
    @Inject
    private VolumeDao _volsDao;
    @Inject
    private SnapshotDao _snapshotDao;
    @Inject
    private SnapshotService snapshotSrv;
    @Inject
    private SnapshotDataFactory snapshotFactory;
    @Inject
    private SnapshotDetailsDao _snapshotDetailsDao;

    private volatile long _executionRunNumber = 1;

    private final ScheduledExecutorService _heartbeatScheduler = Executors.newScheduledThreadPool(1, new NamedThreadFactory("AsyncJobMgr-Heartbeat"));
    private ExecutorService _apiJobExecutor;
    private ExecutorService _workerJobExecutor;

    private boolean asyncJobsEnabled = true;

    @Override
    public String getConfigComponentName() {
        return AsyncJobManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {JobExpireMinutes, JobCancelThresholdMinutes, VmJobLockTimeout, HidePassword};
    }

    @Override
    public AsyncJobVO getAsyncJob(long jobId) {
        return _jobDao.findByIdIncludingRemoved(jobId);
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

    private void checkShutdown() {
        if (!isAsyncJobsEnabled()) {
            throw new CloudRuntimeException("A shutdown has been triggered. Can not accept new jobs");
        }
    }

    @SuppressWarnings("unchecked")
    @DB
    public long submitAsyncJob(AsyncJob job, boolean scheduleJobExecutionInContext) {
        checkShutdown();

        @SuppressWarnings("rawtypes")
        GenericDao dao = GenericDaoBase.getDao(job.getClass());
        job.setInitMsid(getMsid());
        job.setExecutingMsid(getMsid());
        job.setSyncSource(null);        // no sync source originally
        dao.persist(job);

        publishOnEventBus(job, "submit");
        scheduleExecution(job, scheduleJobExecutionInContext);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("submit async job-" + job.getId() + ", details: " + StringUtils.cleanString(job.toString()));
        }
        return job.getId();
    }

    @SuppressWarnings("unchecked")
    @Override
    @DB
    public long submitAsyncJob(final AsyncJob job, final String syncObjType, final long syncObjId) {
        checkShutdown();

        try {
            @SuppressWarnings("rawtypes")
            final GenericDao dao = GenericDaoBase.getDao(job.getClass());

            if (dao == null) {
                throw new CloudRuntimeException(String.format("Failed to get dao from job's class=%s, for job id=%d, cmd=%s", job.getClass(), job.getId(), job.getCmd()));
            }

            publishOnEventBus(job, "submit");

            if (!_vmInstanceDao.lockInLockTable(String.valueOf(syncObjId), VmJobLockTimeout.value())){
                throw new CloudRuntimeException("Failed to acquire lock in submitting async job: " + job.getCmd() + " with timeout value = " + VmJobLockTimeout.value());
            }

            try {
                // lock is acquired
                return Transaction.execute(new TransactionCallback<Long>() {
                    @Override
                    public Long doInTransaction(TransactionStatus status) {
                        job.setInitMsid(getMsid());
                        dao.persist(job);

                        syncAsyncJobExecution(job, syncObjType, syncObjId, 1);
                        return job.getId();
                    }
                });
            } finally {
                _vmInstanceDao.unlockFromLockTable(String.valueOf(syncObjId));
            }
        } catch (Exception e) {
            String errMsg = "Unable to schedule async job for command " + job.getCmd() + ", unexpected exception.";
            s_logger.warn(errMsg, e);
            throw new CloudRuntimeException(errMsg);
        }
    }

    @Override
    @DB
    public void completeAsyncJob(final long jobId, final Status jobStatus, final int resultCode, final String resultObject) {
        String resultObj = null;
        if (s_logger.isDebugEnabled()) {
            resultObj = convertHumanReadableJson(obfuscatePassword(resultObject, HidePassword.value()));
            s_logger.debug("Complete async job-" + jobId + ", jobStatus: " + jobStatus + ", resultCode: " + resultCode + ", result: " + resultObj);
        }


        final AsyncJobVO job = _jobDao.findById(jobId);
        if (job == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("job-" + jobId + " no longer exists, we just log completion info here. " + jobStatus + ", resultCode: " + resultCode + ", result: " +
                    resultObj);
            }
            // still purge item from queue to avoid any blocking
            _queueMgr.purgeAsyncJobQueueItemId(jobId);
            return;
        }

        if (job.getStatus() != JobInfo.Status.IN_PROGRESS) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("job-" + jobId + " is already completed.");
            }
            // still purge item from queue to avoid any blocking
            _queueMgr.purgeAsyncJobQueueItemId(jobId);
            return;
        }

        if (resultObject != null) {
            job.setResult(resultObject);
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Publish async job-" + jobId + " complete on message bus");
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Wake up jobs related to job-" + jobId);
        }
        final List<Long> wakeupList = Transaction.execute(new TransactionCallback<List<Long>>() {
            @Override
            public List<Long> doInTransaction(final TransactionStatus status) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Update db status for job-" + jobId);
                }
                job.setCompleteMsid(getMsid());
                job.setStatus(jobStatus);
                job.setResultCode(resultCode);

                if (resultObject != null) {
                    job.setResult(resultObject);
                } else {
                    job.setResult(null);
                }

                final Date currentGMTTime = DateUtil.currentGMTTime();
                job.setLastUpdated(currentGMTTime);
                job.setRemoved(currentGMTTime);
                job.setExecutingMsid(null);
                _jobDao.update(jobId, job);

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Wake up jobs joined with job-" + jobId + " and disjoin all subjobs created from job- " + jobId);
                }
                final List<Long> wakeupList = wakeupByJoinedJobCompletion(jobId);
                _joinMapDao.disjoinAllJobs(jobId);

                // purge the job sync item from queue
                _queueMgr.purgeAsyncJobQueueItemId(jobId);

                return wakeupList;
            }
        });

        publishOnEventBus(job, "complete"); // publish before the instance type and ID are wiped out

        //
        // disable wakeup scheduling now, since all API jobs are currently using block-waiting for sub-jobs
        //
        /*
                for (Long id : wakeupList) {
                    // TODO, we assume that all jobs in this category is API job only
                    AsyncJobVO jobToWakeup = _jobDao.findById(id);
                    if (jobToWakeup != null && (jobToWakeup.getPendingSignals() & AsyncJob.Constants.SIGNAL_MASK_WAKEUP) != 0)
                        scheduleExecution(jobToWakeup, false);
                }
        */
        _messageBus.publish(null, AsyncJob.Topics.JOB_STATE, PublishScope.GLOBAL, jobId);
    }

    private String convertHumanReadableJson(String resultObj) {

        if (resultObj != null && resultObj.contains("/") && resultObj.contains("{")){
            resultObj = resultObj.substring(0, resultObj.indexOf("{")) + getHumanReadableBytesJson(resultObj.substring(resultObj.indexOf("{")));
        }

        return resultObj;
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

        publishOnEventBus(job, "update");
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

        final AsyncJobVO job = _jobDao.findById(jobId);
        publishOnEventBus(job, "update");

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
        _joinMapDao.completeJoin(joinJobId, joinStatus, joinResult, getMsid());
    }

    @Override
    public void syncAsyncJobExecution(AsyncJob job, String syncObjType, long syncObjId, long queueSizeLimit) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Sync job-" + job.getId() + " execution on object " + syncObjType + "." + syncObjId);
        }

        SyncQueueVO queue = null;
        queue = _queueMgr.queue(syncObjType, syncObjId, SyncQueueItem.AsyncJobContentType, job.getId(), queueSizeLimit);
        if (queue == null)
            throw new CloudRuntimeException("Unable to insert queue item into database, DB is full?");
    }

    @Override
    public AsyncJob queryJob(final long jobId, final boolean updatePollTime) {
        final AsyncJobVO job = _jobDao.findByIdIncludingRemoved(jobId);

        if (updatePollTime) {
            job.setLastPolled(DateUtil.currentGMTTime());
            _jobDao.update(jobId, job);
        }
        return job;
    }

    public String obfuscatePassword(String result, boolean hidePassword) {
        if (hidePassword) {
            String pattern = "\"password\":";
            if (result != null) {
                if (result.contains(pattern)) {
                    String[] resp = result.split(pattern);
                    String psswd = resp[1].toString().split(",")[0];
                    if (psswd.endsWith("}")) {
                        psswd = psswd.substring(0, psswd.length() - 1);
                        result = resp[0] + pattern + psswd.replace(psswd.substring(2, psswd.length() - 1), "*****") + "}," + resp[1].split(",", 2)[1];
                    } else {
                        result = resp[0] + pattern + psswd.replace(psswd.substring(2, psswd.length() - 1), "*****") + "," + resp[1].split(",", 2)[1];
                    }
                }
            }
        }
        return result;
    }

    private void scheduleExecution(final AsyncJobVO job) {
        scheduleExecution(job, false);
    }

    private void scheduleExecution(final AsyncJob job, boolean executeInContext) {
        Runnable runnable = getExecutorRunnable(job);
        if (executeInContext) {
            runnable.run();
        } else {
            if (job.getDispatcher() == null || job.getDispatcher().equalsIgnoreCase("ApiAsyncJobDispatcher"))
                _apiJobExecutor.submit(runnable);
            else
                _workerJobExecutor.submit(runnable);
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

    private AsyncJobDispatcher findWakeupDispatcher(AsyncJob job) {
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
            public void run() {
                // register place-holder context to avoid installing system account call context
                if (CallContext.current() == null)
                    CallContext.registerPlaceHolderContext();

                String related = job.getRelated();
                String logContext = job.getShortUuid();
                if (related != null && !related.isEmpty()) {
                    NDC.push("job-" + related + "/" + "job-" + job.getId());
                    AsyncJob relatedJob = _jobDao.findByIdIncludingRemoved(Long.parseLong(related));
                    if (relatedJob != null) {
                        logContext = relatedJob.getShortUuid();
                    }
                } else {
                    NDC.push("job-" + job.getId());
                }
                MDC.put("logcontextid", logContext);
                try {
                    super.run();
                } finally {
                    NDC.pop();
                }
            }

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
                    String related = job.getRelated();
                    String logContext = job.getShortUuid();
                    if (related != null && !related.isEmpty()) {
                        AsyncJob relatedJob = _jobDao.findByIdIncludingRemoved(Long.parseLong(related));
                        if (relatedJob != null) {
                            logContext = relatedJob.getShortUuid();
                        }
                    }
                    MDC.put("logcontextid", logContext);

                    // execute the job
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Executing " + StringUtils.cleanString(job.toString()));
                    }

                    if ((getAndResetPendingSignals(job) & AsyncJob.Constants.SIGNAL_MASK_WAKEUP) != 0) {
                        AsyncJobDispatcher jobDispatcher = findWakeupDispatcher(job);
                        if (jobDispatcher != null) {
                            jobDispatcher.runJob(job);
                        } else {
                            // TODO, job wakeup is not in use yet
                            if (s_logger.isTraceEnabled())
                                s_logger.trace("Unable to find a wakeup dispatcher from the joined job: " + job);
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
                        if (job.getSyncSource() != null) {
                            // here check queue item one more time to double make sure that queue item is removed in case of any uncaught exception
                            _queueMgr.purgeItem(job.getSyncSource().getId());
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

            //
            // TODO: a temporary solution to work-around DB deadlock situation
            //
            // to live with DB deadlocks, we will give a chance for job to be rescheduled
            // in case of exceptions (most-likely DB deadlock exceptions)
            try {
                job.setExecutingMsid(getMsid());
                _jobDao.update(job.getId(), job);
            } catch (Exception e) {
                s_logger.warn("Unexpected exception while dispatching job-" + item.getContentId(), e);

                try {
                    _queueMgr.returnItem(item.getId());
                } catch (Throwable thr) {
                    s_logger.error("Unexpected exception while returning job-" + item.getContentId() + " to queue", thr);
                }
            }

            try {
                scheduleExecution(job);
            } catch (RejectedExecutionException e) {
                s_logger.warn("Execution for job-" + job.getId() + " is rejected, return it to the queue for next turn");

                try {
                    _queueMgr.returnItem(item.getId());
                } catch (Exception e2) {
                    s_logger.error("Unexpected exception while returning job-" + item.getContentId() + " to queue", e2);
                }

                try {
                    job.setExecutingMsid(null);
                    _jobDao.update(job.getId(), job);
                } catch (Exception e3) {
                    s_logger.warn("Unexpected exception while update job-" + item.getContentId() + " msid for bookkeeping");
                }
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
                s_logger.debug("Release sync source for job-" + executionContext.getJob().getId() + " sync source: " + executionContext.getSyncSource().getContentType() +
                    "-" + executionContext.getSyncSource().getContentId());
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
            while (timeoutInMiliseconds < 0 || System.currentTimeMillis() - startTick < timeoutInMiliseconds) {
                msgDetector.waitAny(checkIntervalInMilliSeconds);
                job = _jobDao.findById(job.getId());
                if (job != null && job.getStatus().done()) {
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

    @Override
    public String marshallResultObject(Serializable obj) {
        if (obj != null)
            return JobSerializerHelper.toObjectSerializedString(obj);

        return null;
    }

    @Override
    public Object unmarshallResultObject(AsyncJob job) {
        if(job != null && job.getResult() != null)
            return JobSerializerHelper.fromObjectSerializedString(job.getResult());
        return null;
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
                GlobalLock scanLock = GlobalLock.getInternLock("AsyncJobManagerHeartbeat");
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

            protected void reallyRun() {
                try {
                    if (!isAsyncJobsEnabled()) {
                        s_logger.info("A shutdown has been triggered. Not executing any async job");
                        return;
                    }

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

                    // forcefully cancel blocking queue items if they've been staying there for too long
                    List<SyncQueueItemVO> blockItems = _queueMgr.getBlockedQueueItems(JobCancelThresholdMinutes.value() * 60000, false);
                    if (blockItems != null && blockItems.size() > 0) {
                        for (SyncQueueItemVO item : blockItems) {
                            try {
                                if (item.getContentType().equalsIgnoreCase(SyncQueueItem.AsyncJobContentType)) {
                                    s_logger.info("Remove Job-" + item.getContentId() + " from Queue-" + item.getId() + " since it has been blocked for too long");
                                    completeAsyncJob(item.getContentId(), JobInfo.Status.FAILED, 0, "Job is cancelled as it has been blocking others for too long");

                                    _jobMonitor.unregisterByJobId(item.getContentId());
                                }

                                // purge the item and resume queue processing
                                _queueMgr.purgeItem(item.getId());
                            } catch (Throwable e) {
                                s_logger.error("Unexpected exception when trying to remove job from sync queue, ", e);
                            }
                        }
                    }

                    Date cutTime = new Date(DateUtil.currentGMTTime().getTime() - JobExpireMinutes.value() * 60000);
                    // limit to 100 jobs per turn, this gives cleanup throughput as 600 jobs per minute
                    // hopefully this will be fast enough to balance potential growth of job table
                    // 1) Expire unfinished jobs that weren't processed yet
                    List<AsyncJobVO> unfinishedJobs = _jobDao.getExpiredUnfinishedJobs(cutTime, 100);
                    for (AsyncJobVO job : unfinishedJobs) {
                        try {
                            s_logger.info("Expunging unfinished job-" + job.getId());

                            _jobMonitor.unregisterByJobId(job.getId());
                            expungeAsyncJob(job);
                        } catch (Throwable e) {
                            s_logger.error("Unexpected exception when trying to expunge job-" + job.getId(), e);
                        }
                    }

                    // 2) Expunge finished jobs
                    List<AsyncJobVO> completedJobs = _jobDao.getExpiredCompletedJobs(cutTime, 100);
                    for (AsyncJobVO job : completedJobs) {
                        try {
                            s_logger.info("Expunging completed job-" + job.getId());

                            expungeAsyncJob(job);
                        } catch (Throwable e) {
                            s_logger.error("Unexpected exception when trying to expunge job-" + job.getId(), e);
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
                if ("VmWork".equals(job.getType())) {
                    _vmWorkJobDao.expunge(job.getId());
                }
                _jobDao.expunge(job.getId());
                // purge corresponding sync queue item
                _queueMgr.purgeAsyncJobQueueItemId(job.getId());
            }
        });
    }

    private long getMsid() {
        return ManagementServerNode.getManagementServerId();
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

            AsyncJobVO job = _jobDao.createForUpdate();
            job.setPendingSignals(AsyncJob.Constants.SIGNAL_MASK_WAKEUP);
            _jobDao.update(job, jobsSC);

            SyncQueueItemVO item = _queueItemDao.createForUpdate();
            item.setLastProcessNumber(null);
            item.setLastProcessMsid(null);
            _queueItemDao.update(item, queueItemsSC);
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

            int apiPoolSize = cloudMaxActive / 2;
            int workPoolSize = (cloudMaxActive * 2) / 3;

            s_logger.info("Start AsyncJobManager API executor thread pool in size " + apiPoolSize);
            _apiJobExecutor = Executors.newFixedThreadPool(apiPoolSize, new NamedThreadFactory(AsyncJobManager.API_JOB_POOL_THREAD_PREFIX));

            s_logger.info("Start AsyncJobManager Work executor thread pool in size " + workPoolSize);
            _workerJobExecutor = Executors.newFixedThreadPool(workPoolSize, new NamedThreadFactory(AsyncJobManager.WORK_JOB_POOL_THREAD_PREFIX));
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

    private void cleanupLeftOverJobs(final long msid) {
        try {
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    // purge sync queue item running on this ms node
                    _queueMgr.cleanupActiveQueueItems(msid, true);
                    // reset job status for all jobs running on this ms node
                    final List<AsyncJobVO> jobs = _jobDao.getResetJobs(msid);
                    for (final AsyncJobVO job : jobs) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Cancel left-over job-" + job.getId());
                        }
                        job.setStatus(JobInfo.Status.FAILED);
                        job.setResultCode(ApiErrorCode.INTERNAL_ERROR.getHttpCode());
                        job.setResult("job cancelled because of management server restart or shutdown");
                        job.setCompleteMsid(msid);
                        final Date currentGMTTime = DateUtil.currentGMTTime();
                        job.setLastUpdated(currentGMTTime);
                        job.setRemoved(currentGMTTime);
                        _jobDao.update(job.getId(), job);
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Purge queue item for cancelled job-" + job.getId());
                        }
                        _queueMgr.purgeAsyncJobQueueItemId(job.getId());
                        if (ApiCommandResourceType.Volume.toString().equals(job.getInstanceType())) {

                            try {
                                _volumeDetailsDao.removeDetail(job.getInstanceId(), "SNAPSHOT_ID");
                                _volsDao.remove(job.getInstanceId());
                            } catch (Exception e) {
                                s_logger.error("Unexpected exception while removing concurrent request meta data :" + e.getLocalizedMessage());
                            }
                        }
                    }
                    final List<SnapshotDetailsVO> snapshotList = _snapshotDetailsDao.findDetails(AsyncJob.Constants.MS_ID, Long.toString(msid), false);
                    for (final SnapshotDetailsVO snapshotDetailsVO : snapshotList) {
                        SnapshotInfo snapshot = snapshotFactory.getSnapshot(snapshotDetailsVO.getResourceId(), DataStoreRole.Primary);
                        if (snapshot == null) {
                            _snapshotDetailsDao.remove(snapshotDetailsVO.getId());
                            continue;
                        }
                        snapshotSrv.processEventOnSnapshotObject(snapshot, Snapshot.Event.OperationFailed);
                        _snapshotDetailsDao.removeDetail(snapshotDetailsVO.getResourceId(), AsyncJob.Constants.MS_ID);
                    }
                }
            });
        } catch (Throwable e) {
            s_logger.warn("Unexpected exception in cleaning up left over jobs for mamagement server node " + msid, e);
        }
    }

    @Override
    public void onManagementNodeJoined(List<? extends ManagementServerHost> nodeList, long selfNodeId) {
    }

    @Override
    public void onManagementNodeLeft(List<? extends ManagementServerHost> nodeList, long selfNodeId) {
        for (final ManagementServerHost msHost : nodeList) {
            cleanupLeftOverJobs(msHost.getId());
        }
    }

    @Override
    public void onManagementNodeIsolated() {
    }

    @Override
    public boolean start() {
        cleanupLeftOverJobs(getMsid());

        _heartbeatScheduler.scheduleAtFixedRate(getHeartbeatTask(), HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
        _heartbeatScheduler.scheduleAtFixedRate(getGCTask(), GC_INTERVAL, GC_INTERVAL, TimeUnit.MILLISECONDS);

        return true;
    }

    @Override
    public boolean stop() {
        _heartbeatScheduler.shutdown();
        _apiJobExecutor.shutdown();
        _workerJobExecutor.shutdown();
        return true;
    }

    private GenericSearchBuilder<SyncQueueItemVO, Long> ContentIdsSearch;
    private GenericSearchBuilder<AsyncJobJoinMapVO, Long> JoinJobSearch;
    private SearchBuilder<AsyncJobVO> JobIdsSearch;
    private SearchBuilder<SyncQueueItemVO> QueueJobIdsSearch;
    private GenericSearchBuilder<AsyncJobJoinMapVO, Long> JoinJobIdsSearch;
    private GenericSearchBuilder<AsyncJobJoinMapVO, Long> JoinJobTimeSearch;

    protected AsyncJobManagerImpl() {
        // override default run level for manager components to start this early, otherwise, VirtualMachineManagerImpl will
        // get stuck in non-initializing job queue
        setRunLevel(ComponentLifecycle.RUN_LEVEL_FRAMEWORK);
    }

    private void publishOnEventBus(AsyncJob job, String jobEvent) {
        _messageBus.publish(null, AsyncJob.Topics.JOB_EVENT_PUBLISH, PublishScope.LOCAL,
            new Pair<AsyncJob, String>(job, jobEvent));
    }

    @Override
    public List<AsyncJobVO> findFailureAsyncJobs(String... cmds) {
        return _jobDao.getFailureJobsSinceLastMsStart(getMsid(), cmds);
    }

    @Override
    public long countPendingJobs(String havingInfo, String... cmds) {
        return _jobDao.countPendingJobs(havingInfo, cmds);
    }

    // Returns the number of pending jobs for the given Management server msids.
    // NOTE: This is the msid and NOT the id
    @Override
    public long countPendingNonPseudoJobs(Long... msIds) {
    return _jobDao.countPendingNonPseudoJobs(msIds);
    }

    @Override
    public void enableAsyncJobs() {
        this.asyncJobsEnabled = true;
    }

    @Override
    public void disableAsyncJobs() {
        this.asyncJobsEnabled = false;
    }

    @Override
    public boolean isAsyncJobsEnabled() {
        return asyncJobsEnabled;
    }
}
