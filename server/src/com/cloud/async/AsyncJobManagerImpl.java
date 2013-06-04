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

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.command.user.job.QueryAsyncJobResultCmd;
import org.apache.cloudstack.api.response.ExceptionResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobConstants;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobJoinMapVO;
import org.apache.cloudstack.framework.jobs.AsyncJobJournalVO;
import org.apache.cloudstack.framework.jobs.AsyncJobMBeanImpl;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.AsyncJobMonitor;
import org.apache.cloudstack.framework.jobs.AsyncJobVO;
import org.apache.cloudstack.framework.jobs.SyncQueueItem;
import org.apache.cloudstack.framework.jobs.SyncQueueItemVO;
import org.apache.cloudstack.framework.jobs.SyncQueueManager;
import org.apache.cloudstack.framework.jobs.SyncQueueVO;
import org.apache.cloudstack.framework.jobs.dao.AsyncJobDao;
import org.apache.cloudstack.framework.jobs.dao.AsyncJobJoinMapDao;
import org.apache.cloudstack.framework.jobs.dao.AsyncJobJournalDao;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.MessageDetector;
import org.apache.cloudstack.framework.messagebus.PublishScope;
import org.apache.cloudstack.messagebus.TopicConstants;

import com.cloud.api.ApiSerializerHelper;
import com.cloud.cluster.ClusterManager;
import com.cloud.cluster.ClusterManagerListener;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Predicate;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExceptionUtil;
import com.cloud.utils.mgmt.JmxUtil;
import com.cloud.utils.net.MacAddress;

public class AsyncJobManagerImpl extends ManagerBase implements AsyncJobManager, ClusterManagerListener {
    public static final Logger s_logger = Logger.getLogger(AsyncJobManagerImpl.class);
    private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 3; 	// 3 seconds

    private static final int MAX_ONETIME_SCHEDULE_SIZE = 50;
    private static final int HEARTBEAT_INTERVAL = 2000;
    private static final int GC_INTERVAL = 10000;				// 10 seconds

    @Inject private SyncQueueManager _queueMgr;
    @Inject private ClusterManager _clusterMgr;
    @Inject private AccountManager _accountMgr;
    @Inject private AsyncJobDao _jobDao;
    @Inject private AsyncJobJournalDao _journalDao;
    @Inject private ConfigurationDao _configDao;
    @Inject private AsyncJobJoinMapDao _joinMapDao;
    @Inject private List<AsyncJobDispatcher> _jobDispatchers;
    @Inject private MessageBus _messageBus;
    @Inject private AsyncJobMonitor _jobMonitor;

    // property
    private String defaultDispatcher;
    public String getDefaultDispatcher() {
		return defaultDispatcher;
	}
	public void setDefaultDispatcher(String defaultDispatcher) {
		this.defaultDispatcher = defaultDispatcher;
	}

	private long _jobExpireSeconds = 86400;						// 1 day
    private long _jobCancelThresholdSeconds = 3600;         	// 1 hour (for cancelling the jobs blocking other jobs)

    private final ScheduledExecutorService _heartbeatScheduler =
            Executors.newScheduledThreadPool(1, new NamedThreadFactory("AsyncJobMgr-Heartbeat"));
    private ExecutorService _executor;

    @Override
    public AsyncJobVO getAsyncJob(long jobId) {
        return _jobDao.findById(jobId);
    }

    @Override
    public List<AsyncJobVO> findInstancePendingAsyncJobs(String instanceType, Long accountId) {
        return _jobDao.findInstancePendingAsyncJobs(instanceType, accountId);
    }
    
    @Override @DB
	public AsyncJob getPseudoJob() {
    	AsyncJobVO job = _jobDao.findPseudoJob(Thread.currentThread().getId(), getMsid());
    	if(job == null) {
	    	job = new AsyncJobVO();
	    	job.setAccountId(_accountMgr.getSystemAccount().getId());
	    	job.setUserId(_accountMgr.getSystemUser().getId());
	    	job.setInitMsid(getMsid());
	    	job.setDispatcher(AsyncJobConstants.JOB_DISPATCHER_PSEUDO);
	    	job.setInstanceType(AsyncJobConstants.PSEUDO_JOB_INSTANCE_TYPE);
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
	@Override @DB
    public long submitAsyncJob(AsyncJob job, boolean scheduleJobExecutionInContext) {
        Transaction txt = Transaction.currentTxn();
        try {
        	@SuppressWarnings("rawtypes")
			GenericDao dao = GenericDaoBase.getDao(job.getClass());
        	
            txt.start();
            job.setInitMsid(getMsid());
            dao.persist(job);
            txt.commit();

            // no sync source originally
            job.setSyncSource(null);
            scheduleExecution(job, scheduleJobExecutionInContext);
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("submit async job-" + job.getId() + ", details: " + job.toString());
            }
            return job.getId();
        } catch(Exception e) {
            txt.rollback();
            String errMsg = "Unable to schedule async job for command " + job.getCmd() + ", unexpected exception.";
            s_logger.warn(errMsg, e);
            throw new CloudRuntimeException(errMsg);
        }
    }

    @SuppressWarnings("unchecked")
	@Override @DB
	public long submitAsyncJob(AsyncJob job, String syncObjType, long syncObjId) {
        Transaction txt = Transaction.currentTxn();
        try {
        	@SuppressWarnings("rawtypes")
			GenericDao dao = GenericDaoBase.getDao(job.getClass());
        	
            txt.start();
            job.setInitMsid(getMsid());
            dao.persist(job);

            syncAsyncJobExecution(job, syncObjType, syncObjId, 1);
            txt.commit();
            return job.getId();
        } catch(Exception e) {
            String errMsg = "Unable to schedule async job for command " + job.getCmd() + ", unexpected exception.";
            s_logger.warn(errMsg, e);
            throw new CloudRuntimeException(errMsg);
        }
	}
    
    @Override @DB
    public void completeAsyncJob(long jobId, int jobStatus, int resultCode, Object resultObject) {
        if(s_logger.isDebugEnabled()) {
            s_logger.debug("Complete async job-" + jobId + ", jobStatus: " + jobStatus +
                    ", resultCode: " + resultCode + ", result: " + resultObject);
        }

        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            AsyncJobVO job = _jobDao.findById(jobId);
            if(job == null) {
                if(s_logger.isDebugEnabled()) {
                    s_logger.debug("job-" + jobId + " no longer exists, we just log completion info here. " + jobStatus +
                            ", resultCode: " + resultCode + ", result: " + resultObject);
                }

                txn.rollback();
                return;
            }
            
            if(job.getStatus() != AsyncJobConstants.STATUS_IN_PROGRESS) {
                if(s_logger.isDebugEnabled()) {
                    s_logger.debug("job-" + jobId + " is already completed.");
                }
            	
            	txn.rollback();
            	return;
            }

            job.setCompleteMsid(getMsid());
            job.setStatus(jobStatus);
            job.setResultCode(resultCode);

            // reset attached object
            job.setInstanceType(null);
            job.setInstanceId(null);

            if (resultObject != null) {
                job.setResult(ApiSerializerHelper.toSerializedString(resultObject));
            }

            job.setLastUpdated(DateUtil.currentGMTTime());
            _jobDao.update(jobId, job);
            
        	List<Long> wakeupList = _joinMapDao.wakeupByJoinedJobCompletion(jobId);
            _joinMapDao.disjoinAllJobs(jobId);
            
            txn.commit();

            for(Long id : wakeupList) {
            	// TODO, we assume that all jobs in this category is API job only
            	AsyncJobVO jobToWakeup = _jobDao.findById(id);
            	if(jobToWakeup != null && (jobToWakeup.getPendingSignals() & AsyncJobConstants.SIGNAL_MASK_WAKEUP) != 0)
            	    scheduleExecution(jobToWakeup, false);
            }
             
            _messageBus.publish(null, TopicConstants.JOB_STATE, PublishScope.GLOBAL, jobId);
        } catch(Exception e) {
            s_logger.error("Unexpected exception while completing async job-" + jobId, e);
            txn.rollback();
        }
    }

    @Override @DB
    public void updateAsyncJobStatus(long jobId, int processStatus, Object resultObject) {
        if(s_logger.isDebugEnabled()) {
            s_logger.debug("Update async-job progress, job-" + jobId + ", processStatus: " + processStatus +
                    ", result: " + resultObject);
        }

        Transaction txt = Transaction.currentTxn();
        try {
            txt.start();
            AsyncJobVO job = _jobDao.findById(jobId);
            if(job == null) {
                if(s_logger.isDebugEnabled()) {
                    s_logger.debug("job-" + jobId + " no longer exists, we just log progress info here. progress status: " + processStatus);
                }

                txt.rollback();
                return;
            }

            job.setProcessStatus(processStatus);
            if(resultObject != null) {
                job.setResult(ApiSerializerHelper.toSerializedString(resultObject));
            }
            job.setLastUpdated(DateUtil.currentGMTTime());
            _jobDao.update(jobId, job);
            txt.commit();
        } catch(Exception e) {
            s_logger.error("Unexpected exception while updating async job-" + jobId + " status: ", e);
            txt.rollback();
        }
    }

    @Override @DB
    public void updateAsyncJobAttachment(long jobId, String instanceType, Long instanceId) {
        if(s_logger.isDebugEnabled()) {
            s_logger.debug("Update async-job attachment, job-" + jobId + ", instanceType: " + instanceType +
                    ", instanceId: " + instanceId);
        }

        Transaction txt = Transaction.currentTxn();
        try {
            txt.start();

            AsyncJobVO job = _jobDao.createForUpdate();
            job.setInstanceType(instanceType);
            job.setInstanceId(instanceId);
            job.setLastUpdated(DateUtil.currentGMTTime());
            _jobDao.update(jobId, job);

            txt.commit();
        } catch(Exception e) {
            s_logger.error("Unexpected exception while updating async job-" + jobId + " attachment: ", e);
            txt.rollback();
        }
    }
    
    @Override @DB
    public void logJobJournal(long jobId, AsyncJob.JournalType journalType, String
        journalText, String journalObjJson) {
    	AsyncJobJournalVO journal = new AsyncJobJournalVO();
    	journal.setJobId(jobId);
    	journal.setJournalType(journalType);
    	journal.setJournalText(journalText);
    	journal.setJournalObjJsonString(journalObjJson);
    	
    	_journalDao.persist(journal);
    }
    
    @Override @DB
	public void joinJob(long jobId, long joinJobId) {
    	_joinMapDao.joinJob(jobId, joinJobId, getMsid(), 0, 0, null, null, null);
    }
    
    @Override @DB
    public void joinJob(long jobId, long joinJobId, String wakeupHandler, String wakeupDispatcher,
    		String[] wakeupTopcisOnMessageBus, long wakeupIntervalInMilliSeconds, long timeoutInMilliSeconds) {
    	
    	Long syncSourceId = null;
    	AsyncJobExecutionContext context = AsyncJobExecutionContext.getCurrentExecutionContext();
    	assert(context.getJob() != null);
    	if(context.getJob().getSyncSource() != null) {
    		syncSourceId = context.getJob().getSyncSource().getQueueId();
    	}
    	
    	_joinMapDao.joinJob(jobId, joinJobId, getMsid(),
    		wakeupIntervalInMilliSeconds, timeoutInMilliSeconds,
    		syncSourceId, wakeupHandler, wakeupDispatcher);
    }
    
    @Override @DB
    public void disjoinJob(long jobId, long joinedJobId) {
    	_joinMapDao.disjoinJob(jobId, joinedJobId);
    }
    
    @Override @DB
    public void completeJoin(long joinJobId, int joinStatus, String joinResult) {
    	_joinMapDao.completeJoin(joinJobId, joinStatus, joinResult, getMsid());
    }
    
    @Override
    public void syncAsyncJobExecution(AsyncJob job, String syncObjType, long syncObjId, long queueSizeLimit) {
        if(s_logger.isDebugEnabled()) {
            s_logger.debug("Sync job-" + job.getId() + " execution on object " + syncObjType + "." + syncObjId);
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

        if (queue == null)
            throw new CloudRuntimeException("Unable to insert queue item into database, DB is full?");
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
                throw new PermissionDeniedException("Account " + caller + " is not authorized to see job id=" + job.getId());
            }
        } else if (caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
            _accountMgr.checkAccess(caller, null, true, jobOwner);
        }

        //poll the job
        queryAsyncJobResult(cmd.getId());
        return _jobDao.findById(cmd.getId());
    }

    @DB
    public AsyncJobResult queryAsyncJobResult(long jobId) {
        if(s_logger.isTraceEnabled()) {
            s_logger.trace("Query async-job status, job-" + jobId);
        }

        Transaction txt = Transaction.currentTxn();
        AsyncJobResult jobResult = new AsyncJobResult(jobId);

        try {
            txt.start();
            AsyncJobVO job = _jobDao.findById(jobId);
            if(job != null) {
                jobResult.setJobStatus(job.getStatus());
                jobResult.setProcessStatus(job.getProcessStatus());
                jobResult.setResult(job.getResult());
                jobResult.setResultCode(job.getResultCode());
                jobResult.setUuid(job.getUuid());

                if(job.getStatus() == AsyncJobConstants.STATUS_SUCCEEDED ||
                        job.getStatus() == AsyncJobConstants.STATUS_FAILED) {

                    if(s_logger.isDebugEnabled()) {
                        s_logger.debug("Async job-" + jobId + " completed");
                    }
                } else {
                    job.setLastPolled(DateUtil.currentGMTTime());
                    _jobDao.update(jobId, job);
                }
            } else {
                if(s_logger.isDebugEnabled()) {
                    s_logger.debug("Async job-" + jobId + " does not exist, invalid job id?");
                }

                jobResult.setJobStatus(AsyncJobConstants.STATUS_FAILED);
                jobResult.setResult("job-" + jobId + " does not exist");
            }
            txt.commit();
        } catch(Exception e) {
            s_logger.error("Unexpected exception while querying async job-" + jobId + " status: ", e);

            jobResult.setJobStatus(AsyncJobConstants.STATUS_FAILED);
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

    private void scheduleExecution(final AsyncJob job, boolean executeInContext) {
        Runnable runnable = getExecutorRunnable(this, job);
        if (executeInContext) {
            runnable.run();
        } else {
            _executor.submit(runnable);
        }
    }
    
    private AsyncJobDispatcher getDispatcher(String dispatcherName) {
    	if(dispatcherName == null || dispatcherName.isEmpty())
    		dispatcherName = defaultDispatcher;
    	
    	if(_jobDispatchers != null) {
    		for(AsyncJobDispatcher dispatcher : _jobDispatchers) {
    			if(dispatcherName.equals(dispatcher.getName()))
    				return dispatcher;
    		}
    	}
    	return null;
    }
    
    private AsyncJobDispatcher getWakeupDispatcher(AsyncJob job) {
    	if(_jobDispatchers != null) {
    		List<AsyncJobJoinMapVO> joinRecords = _joinMapDao.listJoinRecords(job.getId());
    		if(joinRecords.size() > 0) {
    			AsyncJobJoinMapVO joinRecord = joinRecords.get(0);
	    		for(AsyncJobDispatcher dispatcher : _jobDispatchers) {
	    			if(dispatcher.getName().equals(joinRecord.getWakeupDispatcher()))
	    				return dispatcher;
	    		}
    		} else {
    			s_logger.warn("job-" + job.getId() + " is scheduled for wakeup run, but there is no joining info anymore");
    		}
    	}
    	return null;
    }
    
    private Runnable getExecutorRunnable(final AsyncJobManager mgr, final AsyncJob job) {
        return new Runnable() {
            @Override
            public void run() {
            	Transaction txn = null;
            	try {
            		//
            		// setup execution environment
            		//
                    NDC.push("job-" + job.getId());
                    
            		txn = Transaction.open(Transaction.CLOUD_DB);
            		
                    try {
                        JmxUtil.registerMBean("AsyncJobManager", "Active Job " + job.getId(), new AsyncJobMBeanImpl(job));
                    } catch(Exception e) {
                        s_logger.warn("Unable to register active job " + job.getId() + " to JMX monitoring due to exception " + ExceptionUtil.toString(e));
                    }
                    
                    _jobMonitor.registerActiveTask(job.getId());
                    AsyncJobExecutionContext.setCurrentExecutionContext(
                    	(AsyncJobExecutionContext)ComponentContext.inject(new AsyncJobExecutionContext(job))
                    );
                    
                    // execute the job
                    if(s_logger.isDebugEnabled()) {
                        s_logger.debug("Executing " + job.getCmd() + " for job-" + job.getId());
                    }

                    if((getAndResetPendingSignals(job) & AsyncJobConstants.SIGNAL_MASK_WAKEUP) != 0) {
                    	AsyncJobDispatcher jobDispatcher = getWakeupDispatcher(job);
                    	if(jobDispatcher != null) {
                    		jobDispatcher.runJob(job);
                    	} else {
                    		s_logger.error("Unable to find a wakeup dispatcher from the joined job. job-" + job.getId());
                    	}
                    } else {
	                    AsyncJobDispatcher jobDispatcher = getDispatcher(job.getDispatcher());
	                    if(jobDispatcher != null) {
	                    	jobDispatcher.runJob(job);
	                    } else {
	                    	s_logger.error("Unable to find job dispatcher, job will be cancelled");
	                        completeAsyncJob(job.getId(), AsyncJobConstants.STATUS_FAILED, ApiErrorCode.INTERNAL_ERROR.getHttpCode(), null);
	                    }
                    }
                    
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Done executing " + job.getCmd() + " for job-" + job.getId());
                    }
                   
            	} catch (Throwable e) {
            		s_logger.error("Unexpected exception", e);
                    completeAsyncJob(job.getId(), AsyncJobConstants.STATUS_FAILED, ApiErrorCode.INTERNAL_ERROR.getHttpCode(), null);
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

                    	//
                    	// clean execution environment
                    	//
                        AsyncJobExecutionContext.setCurrentExecutionContext(null);
                        _jobMonitor.unregisterActiveTask(job.getId());
                   	
                    	try {
                    		JmxUtil.unregisterMBean("AsyncJobManager", "Active Job " + job.getId());
                    	} catch(Exception e) {
                            s_logger.warn("Unable to unregister job " + job.getId() + " to JMX monitoring due to exception " + ExceptionUtil.toString(e));
                    	}
                    	
	                    if(txn != null)
	                    	txn.close();
	                    
	                    NDC.pop();
                    } catch(Throwable e) {
                		s_logger.error("Double exception", e);
                    }
            	}
            }
        };
    }
    
    private int getAndResetPendingSignals(AsyncJob job) {
    	int signals = job.getPendingSignals();
    	if(signals != 0) {
	    	AsyncJobVO jobRecord = _jobDao.findById(job.getId());
	    	jobRecord.setPendingSignals(0);
	    	_jobDao.update(job.getId(), jobRecord);
    	}
    	return signals;
    }
    
    private void executeQueueItem(SyncQueueItemVO item, boolean fromPreviousSession) {
        AsyncJobVO job = _jobDao.findById(item.getContentId());
        if (job != null) {
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("Schedule queued job-" + job.getId());
            }

            job.setSyncSource(item);
            
            job.setExecutingMsid(getMsid());
            _jobDao.update(job.getId(), job);

            try {
                scheduleExecution(job);
            } catch(RejectedExecutionException e) {
                s_logger.warn("Execution for job-" + job.getId() + " is rejected, return it to the queue for next turn");
                _queueMgr.returnItem(item.getId());
                
            	job.setExecutingMsid(null);
            	_jobDao.update(job.getId(), job);
            }

        } else {
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find related job for queue item: " + item.toString());
            }

            _queueMgr.purgeItem(item.getId());
        }
    }

    @Override
    public void releaseSyncSource() {
    	AsyncJobExecutionContext executionContext = AsyncJobExecutionContext.getCurrentExecutionContext();
    	assert(executionContext != null);
    	
    	if(executionContext.getSyncSource() != null) {
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("Release sync source for job-" + executionContext.getJob().getId() + " sync source: "
                        + executionContext.getSyncSource().getContentType() + "-"
                        + executionContext.getSyncSource().getContentId());
            }

            _queueMgr.purgeItem(executionContext.getSyncSource().getId());
            checkQueue(executionContext.getSyncSource().getQueueId());
    	}
    }
    
    @Override
    public boolean waitAndCheck(String[] wakupTopicsOnMessageBus, long checkIntervalInMilliSeconds,
        long timeoutInMiliseconds, Predicate predicate) {
    	
    	MessageDetector msgDetector = new MessageDetector();
    	msgDetector.open(_messageBus, wakupTopicsOnMessageBus);
    	try {
    		long startTick = System.currentTimeMillis();
    		while(System.currentTimeMillis() - startTick < timeoutInMiliseconds) {
    			msgDetector.waitAny(checkIntervalInMilliSeconds);
    			if(predicate.checkCondition())
    				return true;
    		}
    	} finally {
    		msgDetector.close();
    	}
    	
    	return false;
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
            	Transaction txn = Transaction.open("AsyncJobManagerImpl.getHeartbeatTask");
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
              
                    List<Long> standaloneWakeupJobs = _joinMapDao.wakeupScan();
                    for(Long jobId : standaloneWakeupJobs) {
                    	// TODO, we assume that all jobs in this category is API job only
                    	AsyncJobVO job = _jobDao.findById(jobId);
                    	if(job != null && (job.getPendingSignals() & AsyncJobConstants.SIGNAL_MASK_WAKEUP) != 0)
                    	    scheduleExecution(job, false);
                    }
                } catch(Throwable e) {
                    s_logger.error("Unexpected exception when trying to execute queue item, ", e);
                } finally {
                	try {
                		txn.close();
                	} catch(Throwable e) {
                        s_logger.error("Unexpected exception", e);
                	}
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
                                completeAsyncJob(item.getContentId(), AsyncJobConstants.STATUS_FAILED, 0,
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
                        completeAsyncJob(jobId, AsyncJobConstants.STATUS_FAILED, 0, getResetResultResponse("Execution was cancelled because of server shutdown"));
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
            _executor = Executors.newFixedThreadPool(poolSize, new NamedThreadFactory(AsyncJobConstants.JOB_POOL_THREAD_PREFIX));
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
        	_jobDao.cleanupPseduoJobs(getMsid());
        	
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
        return ApiSerializerHelper.toSerializedString(getResetResultResponse(errorMessage));
    }

    @Override
    public boolean stop() {
        _heartbeatScheduler.shutdown();
        _executor.shutdown();
        return true;
    }
}
