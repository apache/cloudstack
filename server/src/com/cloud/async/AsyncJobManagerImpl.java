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

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.job.QueryAsyncJobResultCmd;
import org.apache.cloudstack.api.response.ExceptionResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.events.EventBus;
import org.apache.cloudstack.framework.events.EventBusException;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.ApiDispatcher;
import com.cloud.api.ApiGsonHelper;
import com.cloud.api.ApiSerializerHelper;
import com.cloud.async.dao.AsyncJobDao;
import com.cloud.cluster.ClusterManager;
import com.cloud.cluster.ClusterManagerListener;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dao.EntityManager;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.EventCategory;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.server.ManagementServer;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExceptionUtil;
import com.cloud.utils.mgmt.JmxUtil;
import com.cloud.utils.net.MacAddress;

@Component
@Local(value={AsyncJobManager.class})
public class AsyncJobManagerImpl extends ManagerBase implements AsyncJobManager, ClusterManagerListener {
    public static final Logger s_logger = Logger.getLogger(AsyncJobManagerImpl.class.getName());
    private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 3; 	// 3 seconds

    private static final int MAX_ONETIME_SCHEDULE_SIZE = 50;
    private static final int HEARTBEAT_INTERVAL = 2000;
    private static final int GC_INTERVAL = 10000;				// 10 seconds

    @Inject private AsyncJobExecutorContext _context;
    @Inject private SyncQueueManager _queueMgr;
    @Inject private ClusterManager _clusterMgr;
    @Inject private AccountManager _accountMgr;
    @Inject private AccountDao _accountDao;
    @Inject private AsyncJobDao _jobDao;
    @Inject private ConfigurationDao _configDao;
    @Inject private DomainDao _domainDao;
    private long _jobExpireSeconds = 86400;						// 1 day
    private long _jobCancelThresholdSeconds = 3600;         // 1 hour (for cancelling the jobs blocking other jobs)

    @Inject
    private EntityManager _entityMgr;

    @Inject private ApiDispatcher _dispatcher;

    private final ScheduledExecutorService _heartbeatScheduler =
            Executors.newScheduledThreadPool(1, new NamedThreadFactory("AsyncJobMgr-Heartbeat"));
    private ExecutorService _executor;

    @Override
    public AsyncJobExecutorContext getExecutorContext() {
        return _context;
    }

    @Override
    public AsyncJobVO getAsyncJob(long jobId) {
        return _jobDao.findById(jobId);
    }

    @Override
    public AsyncJobVO findInstancePendingAsyncJob(String instanceType, long instanceId) {
        return _jobDao.findInstancePendingAsyncJob(instanceType, instanceId);
    }

    @Override
    public List<AsyncJobVO> findInstancePendingAsyncJobs(ApiCommandJobType instanceType, Long accountId) {
        return _jobDao.findInstancePendingAsyncJobs(instanceType, accountId);
    }

    private void publishOnEventBus(AsyncJobVO job, String jobEvent) {
        EventBus eventBus = null;
        try {
            eventBus = ComponentContext.getComponent(EventBus.class);
        } catch(NoSuchBeanDefinitionException nbe) {
            return; // no provider is configured to provide events bus, so just return
        }

        // Get the event type from the cmdInfo json string
        String info = job.getCmdInfo();
        String cmdEventType;
        if ( info == null ) {
            cmdEventType = "unknown";
        } else {
            String marker = "\"cmdEventType\"";
            int begin = info.indexOf(marker);
            cmdEventType = info.substring(begin + marker.length() + 2, info.indexOf(",", begin) - 1);
        }

        // For some reason, the instanceType / instanceId are not abstract, which means we may get null values.
        org.apache.cloudstack.framework.events.Event event = new org.apache.cloudstack.framework.events.Event(
                ManagementServer.Name,
                EventCategory.ASYNC_JOB_CHANGE_EVENT.getName(),
                jobEvent,
                ( job.getInstanceType() != null ? job.getInstanceType().toString() : "unknown" ), null);

        User userJobOwner = _accountMgr.getUserIncludingRemoved(job.getUserId());
        Account jobOwner  = _accountMgr.getAccount(userJobOwner.getAccountId());

        Map<String, String> eventDescription = new HashMap<String, String>();
        eventDescription.put("command", job.getCmd());
        eventDescription.put("user", userJobOwner.getUuid());
        eventDescription.put("account", jobOwner.getUuid());
        eventDescription.put("processStatus", "" + job.getProcessStatus());
        eventDescription.put("resultCode", "" + job.getResultCode());
        eventDescription.put("instanceUuid", ApiDBUtils.findJobInstanceUuid(job));
        eventDescription.put("instanceType", ( job.getInstanceType() != null ? job.getInstanceType().toString() : "unknown" ) );
        eventDescription.put("commandEventType", cmdEventType);
        eventDescription.put("jobId", job.getUuid());

        // If the event.accountinfo boolean value is set, get the human readable value for the username / domainname
        Map<String, String> configs = _configDao.getConfiguration("management-server", new HashMap<String, String>());
        if ( Boolean.valueOf(configs.get("event.accountinfo")) ) {
            DomainVO domain = _domainDao.findById(jobOwner.getDomainId());
            eventDescription.put("username", userJobOwner.getUsername());
            eventDescription.put("domainname", domain.getName());
        }

        event.setDescription(eventDescription);

        try {
            eventBus.publish(event);
        } catch (EventBusException evx) {
            String errMsg = "Failed to publish async job event on the the event bus.";
            s_logger.warn(errMsg, evx);
            throw new CloudRuntimeException(errMsg);
        }
    }

    @Override
    public long submitAsyncJob(AsyncJobVO job) {
        return submitAsyncJob(job, false);
    }

    @Override @DB
    public long submitAsyncJob(AsyncJobVO job, boolean scheduleJobExecutionInContext) {
        Transaction txt = Transaction.currentTxn();
        try {
            txt.start();
            job.setInitMsid(getMsid());
            _jobDao.persist(job);
            txt.commit();

            // no sync source originally
            job.setSyncSource(null);
            scheduleExecution(job, scheduleJobExecutionInContext);
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("submit async job-" + job.getId() + " = [ " + job.getUuid() + " ], details: " + job.toString());
            }
            publishOnEventBus(job, "submit");
            return job.getId();
        } catch(Exception e) {
            txt.rollback();
            String errMsg = "Unable to schedule async job for command " + job.getCmd() + ", unexpected exception.";
            s_logger.warn(errMsg, e);
            throw new CloudRuntimeException(errMsg);
        }
    }

    @Override @DB
    public void completeAsyncJob(long jobId, int jobStatus, int resultCode, Object resultObject) {
        AsyncJobVO job = _jobDao.findById(jobId);
        String jobUuid = null;
        if (job != null) {
            jobUuid = job.getUuid();
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("Complete async job-" + jobId + " = [ " + jobUuid + " ], jobStatus: " + jobStatus +
                        ", resultCode: " + resultCode + ", result: " + resultObject);
            }
        } else {
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("job-" + jobId + " no longer exists, we just log completion info here. " + jobStatus +
                        ", resultCode: " + resultCode + ", result: " + resultObject);
            }
            return;
        }

        Transaction txt = Transaction.currentTxn();
        try {
            txt.start();
            job.setCompleteMsid(getMsid());
            job.setStatus(jobStatus);
            job.setResultCode(resultCode);

            publishOnEventBus(job, "complete"); // publish before the instance type and ID are wiped out

            // reset attached object
            job.setInstanceType(null);
            job.setInstanceId(null);

            if (resultObject != null) {
                job.setResult(ApiSerializerHelper.toSerializedStringOld(resultObject));
            }

            job.setLastUpdated(DateUtil.currentGMTTime());
            _jobDao.update(jobId, job);
            txt.commit();
        } catch(Exception e) {
            s_logger.error("Unexpected exception while completing async job-" + jobId + " = [ " + jobUuid + " ]", e);
            txt.rollback();
        }
    }

    @Override @DB
    public void updateAsyncJobStatus(long jobId, int processStatus, Object resultObject) {
        AsyncJobVO job = _jobDao.findById(jobId);
        String jobUuid = null;
        if (job != null) {
            jobUuid = job.getUuid();
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("Update async-job progress, job-" + jobId + " = [ " + jobUuid + " ], processStatus: " + processStatus +
                        ", result: " + resultObject);
            }
        } else {
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("job-" + jobId + " no longer exists, we just log progress info here. progress status: " + processStatus);
            }
            return;
        }

        Transaction txt = Transaction.currentTxn();
        try {
            txt.start();
            job.setProcessStatus(processStatus);
            if(resultObject != null) {
                job.setResult(ApiSerializerHelper.toSerializedStringOld(resultObject));
            }
            job.setLastUpdated(DateUtil.currentGMTTime());
            _jobDao.update(jobId, job);
            publishOnEventBus(job, "update");
            txt.commit();
        } catch(Exception e) {
            s_logger.error("Unexpected exception while updating async job-" + jobId + " = [ " + jobUuid + " ] status: ", e);
            txt.rollback();
        }
    }

    @Override @DB
    public void updateAsyncJobAttachment(long jobId, String instanceType, Long instanceId) {
        AsyncJobVO job = _jobDao.findById(jobId);
        String jobUuid = null;
        if (job != null) {
            jobUuid = job.getUuid();
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("Update async-job attachment, job-" + jobId + " = [ " + jobUuid + " ], instanceType: "
                        + instanceType + ", instanceId: " + instanceId);
            }
        } else {
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("job-" + jobId + " no longer exists, instanceType: " + instanceType + ", instanceId: "
                        + instanceId);
            }
            return;
        }

        Transaction txt = Transaction.currentTxn();
        try {
            txt.start();
            //job.setInstanceType(instanceType);
            job.setInstanceId(instanceId);
            job.setLastUpdated(DateUtil.currentGMTTime());
            _jobDao.update(jobId, job);

            txt.commit();
        } catch(Exception e) {
            s_logger.error("Unexpected exception while updating async job-" + jobId + " = [ " + jobUuid + " ] attachment: ", e);
            txt.rollback();
        }
    }

    @Override
    public void syncAsyncJobExecution(AsyncJob job, String syncObjType, long syncObjId, long queueSizeLimit) {
        // This method is re-entrant.  If an API developer wants to synchronized on an object, e.g. the router,
        // when executing business logic, they will call this method (actually a method in BaseAsyncCmd that calls this).
        // This method will get called every time their business logic executes.  The first time it exectues for a job
        // there will be no sync source, but on subsequent execution there will be a sync souce.  If this is the first
        // time the job executes we queue the job, otherwise we just return so that the business logic can execute.
        if (job.getSyncSource() != null) {
            return;
        }

        if(s_logger.isDebugEnabled()) {
            s_logger.debug("Sync job-" + job.getId() + " = [ " + job.getUuid() + " ] execution on object " + syncObjType + "." + syncObjId);
        }

        SyncQueueVO queue = null;

        // to deal with temporary DB exceptions like DB deadlock/Lock-wait time out cased rollbacks
        // we retry five times until we throw an exception
        Random random = new Random();

        for(int i = 0; i < 5; i++) {
            queue = _queueMgr.queue(syncObjType, syncObjId, SyncQueueItem.AsyncJobContentType, job.getId(), queueSizeLimit);
            if(queue != null) {
                break;
            }

            try {
                Thread.sleep(1000 + random.nextInt(5000));
            } catch (InterruptedException e) {
            }
        }

        if (queue == null) {
            throw new CloudRuntimeException("Unable to insert queue item into database, DB is full?");
        } else {
            throw new AsyncCommandQueued(queue, "job-" + job.getId() + " = [ " + job.getUuid() + " ] queued");
        }
    }

    @Override
    public AsyncJob queryAsyncJobResult(QueryAsyncJobResultCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();

        AsyncJobVO job = _jobDao.findById(cmd.getId());
        if (job == null) {
            throw new InvalidParameterValueException("Unable to find a job by id " + cmd.getId());
        }

        User userJobOwner = _accountMgr.getUserIncludingRemoved(job.getUserId());
        Account jobOwner = _accountMgr.getAccount(userJobOwner.getAccountId());

        //check permissions
        if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL) {
            //regular user can see only jobs he owns
            if (caller.getId() != jobOwner.getId()) {
                throw new PermissionDeniedException("Account " + caller + " is not authorized to see job-" + job.getId() + " = [ " + job.getUuid() + " ]");
            }
        } else if (caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
            _accountMgr.checkAccess(caller, null, true, jobOwner);
        }

        //poll the job
        queryAsyncJobResult(cmd.getId());
        return _jobDao.findById(cmd.getId());
    }

    @Override @DB
    public AsyncJobResult queryAsyncJobResult(long jobId) {
        AsyncJobVO job = _jobDao.findById(jobId);
        String jobUuid = null;
        if (job != null) {
            jobUuid = job.getUuid();
            if(s_logger.isTraceEnabled()) {
                s_logger.trace("Query async-job status, job-" + jobId + " = [ " + jobUuid + " ]");
            }
        } else {
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("Async job-" + jobId + " does not exist, invalid job id?");
            }
        }

        Transaction txt = Transaction.currentTxn();
        AsyncJobResult jobResult = new AsyncJobResult(jobId);

        try {
            txt.start();
            if(job != null) {
                jobResult.setCmdOriginator(job.getCmdOriginator());
                jobResult.setJobStatus(job.getStatus());
                jobResult.setProcessStatus(job.getProcessStatus());
                jobResult.setResult(job.getResult());
                jobResult.setResultCode(job.getResultCode());
                jobResult.setUuid(job.getUuid());

                if(job.getStatus() == AsyncJobResult.STATUS_SUCCEEDED ||
                        job.getStatus() == AsyncJobResult.STATUS_FAILED) {

                    if(s_logger.isDebugEnabled()) {
                        s_logger.debug("Async job-" + jobId + " = [ " + jobUuid + " ] completed");
                    }
                } else {
                    job.setLastPolled(DateUtil.currentGMTTime());
                    _jobDao.update(jobId, job);
                }
            } else {
                jobResult.setJobStatus(AsyncJobResult.STATUS_FAILED);
                jobResult.setResult("job-" + jobId + " does not exist");
            }
            txt.commit();
        } catch(Exception e) {
            if (jobUuid == null) {
                s_logger.error("Unexpected exception while querying async job-" + jobId + " status: ", e);
            } else {
                s_logger.error("Unexpected exception while querying async job-" + jobId + " = [ " + jobUuid + " ] status: ", e);
            }

            jobResult.setJobStatus(AsyncJobResult.STATUS_FAILED);
            jobResult.setResult("Exception: " + e.toString());
            txt.rollback();
        }

        if(s_logger.isTraceEnabled()) {
            s_logger.trace("Job status: " + jobResult.toString());
        }

        return jobResult;
    }

    private void scheduleExecution(final AsyncJobVO job) {
        scheduleExecution(job, false);
    }

    private void scheduleExecution(final AsyncJobVO job, boolean executeInContext) {
        Runnable runnable = getExecutorRunnable(this, job);
        if (executeInContext) {
            runnable.run();
        } else {
            _executor.submit(runnable);
        }
    }

    private Runnable getExecutorRunnable(final AsyncJobManager mgr, final AsyncJobVO job) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    long jobId = 0;

                    try {
                        JmxUtil.registerMBean("AsyncJobManager", "Active Job " + job.getId(), new AsyncJobMBeanImpl(job));
                    } catch(Exception e) {
                        s_logger.warn("Unable to register active job [ " + job.getId() + " ] = [ " + job.getUuid() + " ] to JMX monitoring due to exception " + ExceptionUtil.toString(e));
                    }

                    BaseAsyncCmd cmdObj = null;
                    Transaction txn = Transaction.open(Transaction.CLOUD_DB);
                    try {
                        jobId = job.getId();
                        String jobUuid = job.getUuid();
                        NDC.push("job-" + jobId + " = [ " + jobUuid + " ]");

                        if(s_logger.isDebugEnabled()) {
                            s_logger.debug("Executing " + job.getCmd() + " for job-" + jobId + " = [ " + jobUuid + " ]");
                        }

                        Class<?> cmdClass = Class.forName(job.getCmd());
                        cmdObj = (BaseAsyncCmd)cmdClass.newInstance();
                        cmdObj = ComponentContext.inject(cmdObj);
                        cmdObj.configure();
                        cmdObj.setJob(job);

                        Type mapType = new TypeToken<Map<String, String>>() {}.getType();
                        Gson gson = ApiGsonHelper.getBuilder().create();
                        Map<String, String> params = gson.fromJson(job.getCmdInfo(), mapType);

                        // whenever we deserialize, the UserContext needs to be updated
                        String userIdStr = params.get("ctxUserId");
                        String acctIdStr = params.get("ctxAccountId");
                        Long userId = null;
                        Account accountObject = null;
                        User user = null;

                        if (userIdStr != null) {
                            userId = Long.parseLong(userIdStr);
                            user = _entityMgr.findById(User.class, userId);
                        }

                        if (acctIdStr != null) {
                            accountObject = _accountDao.findById(Long.parseLong(acctIdStr));
                        }


                        CallContext.register(user, accountObject);
                        try {
                            // dispatch could ultimately queue the job
                            _dispatcher.dispatch(cmdObj, params);

                            // serialize this to the async job table
                            completeAsyncJob(jobId, AsyncJobResult.STATUS_SUCCEEDED, 0, cmdObj.getResponseObject());
                        } finally {
                            CallContext.unregister();
                        }

                        // commands might need to be queued as part of synchronization here, so they just have to be re-dispatched from the queue mechanism...
                        if (job.getSyncSource() != null) {
                            _queueMgr.purgeItem(job.getSyncSource().getId());
                            checkQueue(job.getSyncSource().getQueueId());
                        }

                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Done executing " + job.getCmd() + " for job-" + job.getId() + " = [ " + jobUuid + " ]");
                        }

                    } catch(Throwable e) {
                        if (e instanceof AsyncCommandQueued) {
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("job " + job.getCmd() + " for job-" + jobId + " = [ " + job.getUuid() + " ] was queued, processing the queue.");
                            }
                            checkQueue(((AsyncCommandQueued)e).getQueue().getId());
                        } else {
                            String errorMsg = null;
                            int errorCode = ApiErrorCode.INTERNAL_ERROR.getHttpCode();
                            if (!(e instanceof ServerApiException)) {
                                s_logger.error("Unexpected exception while executing " + job.getCmd(), e);
                                errorMsg = e.getMessage();
                            } else {
                                ServerApiException sApiEx = (ServerApiException)e;
                                errorMsg = sApiEx.getDescription();
                                errorCode = sApiEx.getErrorCode().getHttpCode();
                            }

                            ExceptionResponse response = new ExceptionResponse();
                            response.setErrorCode(errorCode);
                            response.setErrorText(errorMsg);
                            response.setResponseName((cmdObj == null) ? "unknowncommandresponse" : cmdObj.getCommandName());

                            // FIXME:  setting resultCode to ApiErrorCode.INTERNAL_ERROR is not right, usually executors have their exception handling
                            //         and we need to preserve that as much as possible here
                            completeAsyncJob(jobId, AsyncJobResult.STATUS_FAILED, ApiErrorCode.INTERNAL_ERROR.getHttpCode(), response);

                            // need to clean up any queue that happened as part of the dispatching and move on to the next item in the queue
                            try {
                                if (job.getSyncSource() != null) {
                                    _queueMgr.purgeItem(job.getSyncSource().getId());
                                    checkQueue(job.getSyncSource().getQueueId());
                                }
                            } catch(Throwable ex) {
                                s_logger.fatal("Exception on exception, log it for record", ex);
                            }
                        }
                    } finally {

                        try {
                            JmxUtil.unregisterMBean("AsyncJobManager", "Active Job " + job.getId());
                        } catch(Exception e) {
                            s_logger.warn("Unable to unregister active job [ " + job.getId() + " ] = [ " + job.getUuid() + " ] from JMX monitoring");
                        }

                        txn.close();
                        NDC.pop();
                    }
                } catch (Throwable th) {
                    try {
                        s_logger.error("Caught: " + th);
                    } catch (Throwable th2) {
                    }
                }
            }
        };
    }

    private void executeQueueItem(SyncQueueItemVO item, boolean fromPreviousSession) {
        long jobId = item.getContentId();
        AsyncJobVO job = _jobDao.findById(item.getContentId());
        if (job != null) {
            String jobUuid = job.getUuid();
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("Schedule queued job-" + jobId + " = [ " + jobUuid + " ]");
            }

            job.setFromPreviousSession(fromPreviousSession);
            job.setSyncSource(item);

            job.setCompleteMsid(getMsid());
            _jobDao.update(job.getId(), job);

            try {
                scheduleExecution(job);
            } catch(RejectedExecutionException e) {
                s_logger.warn("Execution for job-" + jobId + " = [ " + jobUuid + " ] is rejected, return it to the queue for next turn");
                _queueMgr.returnItem(item.getId());
            }

        } else {
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find related job for queue item: " + item.toString());
            }

            _queueMgr.purgeItem(item.getId());
        }
    }

    @Override
    public void releaseSyncSource(AsyncJobExecutor executor) {
        if(executor.getSyncSource() != null) {
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("Release sync source for job-" + executor.getJob().getId() + " = [ " + executor.getJob().getUuid() + " ] sync source: "
                        + executor.getSyncSource().getContentType() + "-"
                        + executor.getSyncSource().getContentId());
            }

            _queueMgr.purgeItem(executor.getSyncSource().getId());
            checkQueue(executor.getSyncSource().getQueueId());
        }
    }

    private void checkQueue(long queueId) {
        while(true) {
            try {
                SyncQueueItemVO item = _queueMgr.dequeueFromOne(queueId, getMsid());
                if(item != null) {
                    if(s_logger.isDebugEnabled()) {
                        s_logger.debug("Executing sync queue item: " + item.toString());
                    }

                    executeQueueItem(item, false);
                } else {
                    break;
                }
            } catch(Throwable e) {
                s_logger.error("Unexpected exception when kicking sync queue-" + queueId, e);
                break;
            }
        }
    }

    private Runnable getHeartbeatTask() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    List<SyncQueueItemVO> l = _queueMgr.dequeueFromAny(getMsid(), MAX_ONETIME_SCHEDULE_SIZE);
                    if(l != null && l.size() > 0) {
                        for(SyncQueueItemVO item: l) {
                            if(s_logger.isDebugEnabled()) {
                                s_logger.debug("Execute sync-queue item: " + item.toString());
                            }
                            executeQueueItem(item, false);
                        }
                    }
                } catch(Throwable e) {
                    s_logger.error("Unexpected exception when trying to execute queue item, ", e);
                }
            }
        };
    }

    @DB
    private Runnable getGCTask() {
        return new Runnable() {
            @Override
            public void run() {
                GlobalLock scanLock = GlobalLock.getInternLock("AsyncJobManagerGC");
                try {
                    if(scanLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
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

                    Date cutTime = new Date(DateUtil.currentGMTTime().getTime() - _jobExpireSeconds*1000);

                    // limit to 100 jobs per turn, this gives cleanup throughput as 600 jobs per minute
                    // hopefully this will be fast enough to balance potential growth of job table
                    //1) Expire unfinished jobs that weren't processed yet
                    List<AsyncJobVO> l = _jobDao.getExpiredUnfinishedJobs(cutTime, 100);
                    for(AsyncJobVO job : l) {
                    	s_logger.trace("Expunging unfinished job " + job);
                        expungeAsyncJob(job);
                    }
                    
                    //2) Expunge finished jobs
                    List<AsyncJobVO> completedJobs = _jobDao.getExpiredCompletedJobs(cutTime, 100);
                    for(AsyncJobVO job : completedJobs) {
                    	s_logger.trace("Expunging completed job " + job);
                        expungeAsyncJob(job);
                    }

                    // forcefully cancel blocking queue items if they've been staying there for too long
                    List<SyncQueueItemVO> blockItems = _queueMgr.getBlockedQueueItems(_jobCancelThresholdSeconds*1000, false);
                    if(blockItems != null && blockItems.size() > 0) {
                        for(SyncQueueItemVO item : blockItems) {
                            if(item.getContentType().equalsIgnoreCase(SyncQueueItem.AsyncJobContentType)) {
                                completeAsyncJob(item.getContentId(), AsyncJobResult.STATUS_FAILED, 0,
                                        getResetResultResponse("Job is cancelled as it has been blocking others for too long"));
                            }

                            // purge the item and resume queue processing
                            _queueMgr.purgeItem(item.getId());
                        }
                    }

                    s_logger.trace("End cleanup expired async-jobs");
                } catch(Throwable e) {
                    s_logger.error("Unexpected exception when trying to execute queue item, ", e);
                }
            }


        };
    }

    @DB
    protected void expungeAsyncJob(AsyncJobVO job) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        _jobDao.expunge(job.getId());
        //purge corresponding sync queue item
        _queueMgr.purgeAsyncJobQueueItemId(job.getId());
        txn.commit();
    }

    private long getMsid() {
        if(_clusterMgr != null) {
            return _clusterMgr.getManagementNodeId();
        }

        return MacAddress.getMacAddress().toLong();
    }

    private void cleanupPendingJobs(List<SyncQueueItemVO> l) {
        if(l != null && l.size() > 0) {
            for(SyncQueueItemVO item: l) {
                if(s_logger.isInfoEnabled()) {
                    s_logger.info("Discard left-over queue item: " + item.toString());
                }

                String contentType = item.getContentType();
                if(contentType != null && contentType.equalsIgnoreCase(SyncQueueItem.AsyncJobContentType)) {
                    Long jobId = item.getContentId();
                    if(jobId != null) {
                        s_logger.warn("Mark job as failed as its correspoding queue-item has been discarded. job id: " + jobId);
                        completeAsyncJob(jobId, AsyncJobResult.STATUS_FAILED, 0, getResetResultResponse("Execution was cancelled because of server shutdown"));
                    }
                }
                _queueMgr.purgeItem(item.getId());
            }
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        int expireMinutes = NumbersUtil.parseInt(
                _configDao.getValue(Config.JobExpireMinutes.key()), 24*60);
        _jobExpireSeconds = (long)expireMinutes*60;

        _jobCancelThresholdSeconds = NumbersUtil.parseInt(
                _configDao.getValue(Config.JobCancelThresholdMinutes.key()), 60);
        _jobCancelThresholdSeconds *= 60;

        try {
            final File dbPropsFile = PropertiesUtil.findConfigFile("db.properties");
            final Properties dbProps = new Properties();
            dbProps.load(new FileInputStream(dbPropsFile));

            final int cloudMaxActive = Integer.parseInt(dbProps.getProperty("db.cloud.maxActive"));

            int poolSize = (cloudMaxActive * 2) / 3;

            s_logger.info("Start AsyncJobManager thread pool in size " + poolSize);
            _executor = Executors.newFixedThreadPool(poolSize, new NamedThreadFactory("Job-Executor"));
        } catch (final Exception e) {
            throw new ConfigurationException("Unable to load db.properties to configure AsyncJobManagerImpl");
        }

        return true;
    }

    @Override
    public void onManagementNodeJoined(List<ManagementServerHostVO> nodeList, long selfNodeId) {
    }

    @Override
    public void onManagementNodeLeft(List<ManagementServerHostVO> nodeList, long selfNodeId) {
        for(ManagementServerHostVO msHost : nodeList) {
            Transaction txn = Transaction.open(Transaction.CLOUD_DB);
            try {
                txn.start();
                List<SyncQueueItemVO> items = _queueMgr.getActiveQueueItems(msHost.getId(), true);
                cleanupPendingJobs(items);
                _jobDao.resetJobProcess(msHost.getId(), ApiErrorCode.INTERNAL_ERROR.getHttpCode(), getSerializedErrorMessage("job cancelled because of management server restart"));
                txn.commit();
            } catch(Throwable e) {
                s_logger.warn("Unexpected exception ", e);
                txn.rollback();
            } finally {
                txn.close();
            }
        }
    }

    @Override
    public void onManagementNodeIsolated() {
    }

    @Override
    public boolean start() {
        try {
            List<SyncQueueItemVO> l = _queueMgr.getActiveQueueItems(getMsid(), false);
            cleanupPendingJobs(l);
            _jobDao.resetJobProcess(getMsid(), ApiErrorCode.INTERNAL_ERROR.getHttpCode(), getSerializedErrorMessage("job cancelled because of management server restart"));
        } catch(Throwable e) {
            s_logger.error("Unexpected exception " + e.getMessage(), e);
        }

        _heartbeatScheduler.scheduleAtFixedRate(getHeartbeatTask(), HEARTBEAT_INTERVAL,
                HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
        _heartbeatScheduler.scheduleAtFixedRate(getGCTask(), GC_INTERVAL,
                GC_INTERVAL, TimeUnit.MILLISECONDS);

        return true;
    }

    private static ExceptionResponse getResetResultResponse(String errorMessage) {
        ExceptionResponse resultObject = new ExceptionResponse();
        resultObject.setErrorCode(ApiErrorCode.INTERNAL_ERROR.getHttpCode());
        resultObject.setErrorText(errorMessage);
        return resultObject;
    }

    private static String getSerializedErrorMessage(String errorMessage) {
        return ApiSerializerHelper.toSerializedStringOld(getResetResultResponse(errorMessage));
    }

    @Override
    public boolean stop() {
        _heartbeatScheduler.shutdown();
        _executor.shutdown();
        return true;
    }
}
