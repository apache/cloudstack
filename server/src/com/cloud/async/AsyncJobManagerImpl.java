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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import com.cloud.async.dao.AsyncJobDao;
import com.cloud.cluster.ClusterManager;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.maid.StackMaid;
import com.cloud.serializer.SerializerHelper;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.MacAddress;

@Local(value={AsyncJobManager.class})
public class AsyncJobManagerImpl implements AsyncJobManager {
    public static final Logger s_logger = Logger.getLogger(AsyncJobManagerImpl.class.getName());
	private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 3; 	// 3 seconds
    
    private static final int MAX_ONETIME_SCHEDULE_SIZE = 50;
    private static final int HEARTBEAT_INTERVAL = 2000;
    private static final int GC_INTERVAL = 10000;				// 10 seconds
    
    private String _name;
    
    private AsyncJobExecutorContext _context;
    private SyncQueueManager _queueMgr;
    private ClusterManager _clusterMgr;
    private AsyncJobDao _jobDao;
    private long _jobExpireSeconds = 86400;						// 1 day

    private final ScheduledExecutorService _heartbeatScheduler =
        Executors.newScheduledThreadPool(1, new NamedThreadFactory("AsyncJobMgr-Heartbeat"));
    private final ExecutorService _executor = Executors.newCachedThreadPool(new NamedThreadFactory("Job-Executor"));

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
	public long submitAsyncJob(AsyncJobVO job) {
    	return submitAsyncJob(job, false);
    }
    
    @Override @DB
    public long submitAsyncJob(AsyncJobVO job, boolean scheduleJobExecutionInContext) {
    	if(s_logger.isDebugEnabled())
    		s_logger.debug("submit async job-" + job.getId() + ", details: " + job.toString());
    	
    	AsyncJobExecutor executor = getJobExecutor(job);
    	if(executor == null) {
    		s_logger.error("Unable to find executor to execute command " + job.getCmd() + " for job-" + job.getId());
    	} else {
        	Transaction txt = Transaction.currentTxn();
        	try {
        		txt.start();
        		job.setInitMsid(getMsid());
        		_jobDao.persist(job);
        		txt.commit();
        		// no sync source originally
        		executor.setSyncSource(null);
        		executor.setJob(job);
        		scheduleExecution(executor, scheduleJobExecutionInContext);
        		return job.getId();
        	} catch(Exception e) {
        		s_logger.error("Unexpected exception: ", e);
        		txt.rollback();
        	}
    	}
    	return 0L;
    }
    
    @Override @DB
    public void completeAsyncJob(long jobId, int jobStatus, int resultCode, Object resultObject) {
    	if(s_logger.isDebugEnabled())
    		s_logger.debug("Complete async job-" + jobId + ", jobStatus: " + jobStatus +
    			", resultCode: " + resultCode + ", result: " + resultObject);
    	
    	Transaction txt = Transaction.currentTxn();
    	try {
    		txt.start();
    		AsyncJobVO job = _jobDao.findById(jobId);
    		if(job == null) {
    	    	if(s_logger.isDebugEnabled())
    	    		s_logger.debug("job-" + jobId + " no longer exists, we just log completion info here. " + jobStatus +
    	    			", resultCode: " + resultCode + ", result: " + resultObject);
    			
    			txt.rollback();
    			return;
    		}
    		
    		job.setCompleteMsid(getMsid());
    		job.setStatus(jobStatus);
    		job.setResultCode(resultCode);
    		
    		// reset attached object
    		job.setInstanceType(null);
    		job.setInstanceId(null);
    		
    		if(resultObject != null)
    			job.setResult(SerializerHelper.toSerializedString(resultObject));
    		job.setLastUpdated(DateUtil.currentGMTTime());
    		_jobDao.update(jobId, job);
    		txt.commit();
    	} catch(Exception e) {
    		s_logger.error("Unexpected exception while completing async job-" + jobId, e);
    		txt.rollback();
    	}
    }
    
    @Override @DB
    public void updateAsyncJobStatus(long jobId, int processStatus, Object resultObject) {
    	if(s_logger.isDebugEnabled())
    		s_logger.debug("Update async-job progress, job-" + jobId + ", processStatus: " + processStatus +
    			", result: " + resultObject);
    	
    	Transaction txt = Transaction.currentTxn();
    	try {
    		txt.start();
    		AsyncJobVO job = _jobDao.findById(jobId);
    		if(job == null) {
    	    	if(s_logger.isDebugEnabled())
    	    		s_logger.debug("job-" + jobId + " no longer exists, we just log progress info here. progress status: " + processStatus);
    			
    			txt.rollback();
    			return;
    		}
    		
    		job.setProcessStatus(processStatus);
    		if(resultObject != null)
    			job.setResult(SerializerHelper.toSerializedString(resultObject));
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
    	if(s_logger.isDebugEnabled())
    		s_logger.debug("Update async-job attachment, job-" + jobId + ", instanceType: " + instanceType +
    			", instanceId: " + instanceId);
    	
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
    
    @Override
    public void syncAsyncJobExecution(long jobId, String syncObjType, long syncObjId) {
    	if(s_logger.isDebugEnabled())
    		s_logger.debug("Sync job-" + jobId + " execution on object " + syncObjType + "." + syncObjId);
    	
    	SyncQueueVO queue = null;
    	
		// to deal with temporary DB exceptions like DB deadlock/Lock-wait time out cased rollbacks
    	// we retry five times until we throw an exception
		Random random = new Random();    		

    	for(int i = 0; i < 5; i++) {
    		queue = _queueMgr.queue(syncObjType, syncObjId, "AsyncJob", jobId);
    		if(queue != null)
    			break;

    		try {
				Thread.sleep(1000 + random.nextInt(5000));
			} catch (InterruptedException e) {
			}
    	}
    	
		if(queue != null) {
    		checkQueue(queue.getId());
		} else {
			throw new CloudRuntimeException("Unable to insert queue item into database, DB is full?");
		}
    }
    
    
    
    @Override @DB
    public AsyncJobResult queryAsyncJobResult(long jobId) {
    	if(s_logger.isTraceEnabled())
    		s_logger.trace("Query async-job status, job-" + jobId);
    	
    	Transaction txt = Transaction.currentTxn();
    	AsyncJobResult jobResult = new AsyncJobResult(jobId);
    	
    	try {
    		txt.start();
    		AsyncJobVO job = _jobDao.findById(jobId);
    		if(job != null) {
    			jobResult.setCmdOriginator(job.getCmdOriginator());
    			jobResult.setJobStatus(job.getStatus());
    			jobResult.setProcessStatus(job.getProcessStatus());
    			jobResult.setResult(job.getResult());
    			jobResult.setResultCode(job.getResultCode());
    			
    			if(job.getStatus() == AsyncJobResult.STATUS_SUCCEEDED ||
    				job.getStatus() == AsyncJobResult.STATUS_FAILED) {
    				
    		    	if(s_logger.isDebugEnabled())
    		    		s_logger.debug("Async job-" + jobId + " completed");
    			} else {
    				job.setLastPolled(DateUtil.currentGMTTime());
    				_jobDao.update(jobId, job);
    			}
    		} else {
    	    	if(s_logger.isDebugEnabled())
    	    		s_logger.debug("Async job-" + jobId + " does not exist, invalid job id?");
    			
    			jobResult.setJobStatus(AsyncJobResult.STATUS_FAILED);
    			jobResult.setResult("job-" + jobId + " does not exist");
    		}
    		txt.commit();
    	} catch(Exception e) {
    		s_logger.error("Unexpected exception while querying async job-" + jobId + " status: ", e);
    		
			jobResult.setJobStatus(AsyncJobResult.STATUS_FAILED);
			jobResult.setResult("Exception: " + e.toString());
    		txt.rollback();
    	}
    	
    	if(s_logger.isTraceEnabled())
    		s_logger.trace("Job status: " + jobResult.toString());
    	
    	return jobResult;
    }
    
    private AsyncJobExecutor getJobExecutor(AsyncJobVO job) {
    	String executorClazzName = "com.cloud.async.executor." + job.getCmd() + "Executor";
    	
		try {
			Class<?> consoleProxyClazz = Class.forName(executorClazzName);
			
			AsyncJobExecutor executor = (AsyncJobExecutor)ComponentLocator.inject(consoleProxyClazz);
			executor.setJob(job);
			executor.setAsyncJobMgr(this);
			return executor;
		} catch (final ClassNotFoundException e) {
			s_logger.error("Unable to load async-job executor class: " + executorClazzName, e);
		}
    	return null;
    }

    private void scheduleExecution(final AsyncJobExecutor executor) {
    	scheduleExecution(executor, false);
    }
    
    private void scheduleExecution(final AsyncJobExecutor executor, boolean executeInContext) {
    	Runnable runnable = getExecutorRunnable(executor);
    	if(executeInContext)
    		runnable.run();
    	else
    		_executor.submit(runnable);
    }
    
    private Runnable getExecutorRunnable(final AsyncJobExecutor executor) {
    	return new Runnable() {
    		public void run() {
    			long jobId = 0;
    			BaseAsyncJobExecutor.setCurrentExecutor(executor);

    			Transaction txn = Transaction.open(Transaction.CLOUD_DB);
    			try {
    				jobId = executor.getJob().getId();
    				NDC.push("job-" + jobId);
    				
        			if(s_logger.isDebugEnabled())
        				s_logger.debug("Executing " + executor.getClass().getName() + " for job-" + jobId);
    				
	    			if(executor.execute()) {
	        			if(s_logger.isTraceEnabled())
	        				s_logger.trace("Executing " + executor.getClass().getName() + " returns true for job-" + jobId);
	    				
		    			if(executor.getSyncSource() != null) {
		    				_queueMgr.purgeItem(executor.getSyncSource().getId());
		    				checkQueue(executor.getSyncSource().getQueueId());
		    			}
	    			} else {
	        			if(s_logger.isTraceEnabled())
	        				s_logger.trace("Executing " + executor.getClass().getName() + " returns false for job-" + jobId);
	    			}
	    			
	    			if(s_logger.isDebugEnabled())
	    				s_logger.debug("Done executing " + executor.getClass().getName() + " for job-" + jobId);
	    			
    			} catch(Throwable e) {
    				s_logger.error("Unexpected exception while executing " + executor.getClass().getName(), e);
    				
    				try {
		    			if(executor.getSyncSource() != null) {
		    				_queueMgr.purgeItem(executor.getSyncSource().getId());
		    				
		    				checkQueue(executor.getSyncSource().getQueueId());
		    			}
    				} catch(Throwable ex) {
    					s_logger.fatal("Exception on exception, log it for record", ex);
    				}
    			} finally {
    				StackMaid.current().exitCleanup();
    				txn.close();
    				NDC.pop();
    			}
    			
    			// leave no trace out after execution for security reason
    			BaseAsyncJobExecutor.setCurrentExecutor(null);
    		}
    	};
    }
    
    private void executeQueueItem(SyncQueueItemVO item, boolean fromPreviousSession) {
			AsyncJobVO job = _jobDao.findById(item.getContentId());
			if(job != null) {
		    	AsyncJobExecutor executor = getJobExecutor(job);
		    	if(executor == null) {
		    		s_logger.error("Unable to find job exectutor for job-" + job.getId());
	    			_queueMgr.purgeItem(item.getId());
		    	} else {
		    		if(s_logger.isDebugEnabled())
		    			s_logger.debug("Schedule queued job-" + job.getId());
		    		
		    		executor.setFromPreviousSession(fromPreviousSession);
		    		executor.setSyncSource(item);
		    		executor.setJob(job);
		    		scheduleExecution(executor);
		    	}
			} else {
				if(s_logger.isDebugEnabled())
					s_logger.debug("Unable to find related job for queue item: " + item.toString());
				
				_queueMgr.purgeItem(item.getId());
			}
    }

    @Override
    public void releaseSyncSource(AsyncJobExecutor executor) {
    	if(executor.getSyncSource() != null) {
    		if(s_logger.isDebugEnabled())
    			s_logger.debug("Release sync source for job-" + executor.getJob().getId() + " sync source: "
					+ executor.getSyncSource().getContentType() + "-"
					+ executor.getSyncSource().getContentId());
    		
			_queueMgr.purgeItem(executor.getSyncSource().getId());
			checkQueue(executor.getSyncSource().getQueueId());
    	}
    }
    
    private void checkQueue(long queueId) {
    	while(true) {
    		try {
	        	SyncQueueItemVO item = _queueMgr.dequeueFromOne(queueId, getMsid());
		    	if(item != null) {
		    		if(s_logger.isDebugEnabled())
		    			s_logger.debug("Executing sync queue item: " + item.toString());
		    		
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
			public void run() {
				try {
					List<SyncQueueItemVO> l = _queueMgr.dequeueFromAny(getMsid(), MAX_ONETIME_SCHEDULE_SIZE);
					if(l != null && l.size() > 0) {
						for(SyncQueueItemVO item: l) {
							if(s_logger.isDebugEnabled())
								s_logger.debug("Execute sync-queue item: " + item.toString());
							executeQueueItem(item, false);
						}
					}
				} catch(Throwable e) {
					s_logger.error("Unexpected exception when trying to execute queue item, ", e);
				} finally {
					StackMaid.current().exitCleanup();
				}
			}
		};
	}
	
	@DB
	private Runnable getGCTask() {
		return new Runnable() {
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
			
			private void reallyRun() {
				try {
					s_logger.trace("Begin cleanup expired async-jobs");
					
					Date cutTime = new Date(DateUtil.currentGMTTime().getTime() - _jobExpireSeconds*1000);
					
					// limit to 100 jobs per turn, this gives cleanup throughput as 600 jobs per minute
					// hopefully this will be fast enough to balance potential growth of job table
					List<AsyncJobVO> l = _jobDao.getExpiredJobs(cutTime, 100);
					if(l != null && l.size() > 0) {
						for(AsyncJobVO job : l) {
							_jobDao.delete(job.getId());
						}
					}
					
					s_logger.trace("End cleanup expired async-jobs");
				} catch(Throwable e) {
					s_logger.error("Unexpected exception when trying to execute queue item, ", e);
				} finally {
					StackMaid.current().exitCleanup();
				}
			}
		};
	}
	
	private long getMsid() {
		if(_clusterMgr != null)
			return _clusterMgr.getId();
		
		return MacAddress.getMacAddress().toLong();
	}
	
	private void startupSanityCheck() {
		List<SyncQueueItemVO> l = _queueMgr.getActiveQueueItems(getMsid());
		if(l != null && l.size() > 0) {
			for(SyncQueueItemVO item: l) {

				if(s_logger.isInfoEnabled())
					s_logger.info("Discard left-over queue item: " + item.toString());
				
				String contentType = item.getContentType();
				if(contentType != null && contentType.equals("AsyncJob")) {
					Long jobId = item.getContentId();
					if(jobId != null) {
						completeAsyncJob(jobId, AsyncJobResult.STATUS_FAILED, 0, "Execution was cancelled because of server shutdown");
					}
				}
    			_queueMgr.purgeItem(item.getId());
			}
		}
	}
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
    	_name = name;
    
		ComponentLocator locator = ComponentLocator.getCurrentLocator();
		
		ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
		if (configDao == null) {
			throw new ConfigurationException("Unable to get the configuration dao.");
		}

		Map<String, String> configs = configDao.getConfiguration("management-server", params);
		
		int expireMinutes = NumbersUtil.parseInt(configs.get("job.expire.minutes"), 24*60);
		_jobExpireSeconds = (long)expireMinutes*60;
				
		_jobDao = locator.getDao(AsyncJobDao.class);
		if (_jobDao == null) {
			throw new ConfigurationException("Unable to get "
					+ AsyncJobDao.class.getName());
		}
		
		_context = 	locator.getManager(AsyncJobExecutorContext.class);
		if (_context == null) {
			throw new ConfigurationException("Unable to get "
					+ AsyncJobExecutorContext.class.getName());
		}
		
		_queueMgr = locator.getManager(SyncQueueManager.class);
		if(_queueMgr == null) {
			throw new ConfigurationException("Unable to get "
					+ SyncQueueManager.class.getName());
		}
		
		_clusterMgr = locator.getManager(ClusterManager.class);
    	return true;
    }

    @Override
    public boolean start() {
    	try {
    		startupSanityCheck();
    	} catch(Throwable e) {
    		s_logger.error("Unexpected exception " + e.getMessage(), e);
    	}
    	
    	_heartbeatScheduler.scheduleAtFixedRate(getHeartbeatTask(), HEARTBEAT_INTERVAL,
			HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
    	_heartbeatScheduler.scheduleAtFixedRate(getGCTask(), GC_INTERVAL,
			GC_INTERVAL, TimeUnit.MILLISECONDS);
    	
        return true;
    }

    @Override
    public boolean stop() {
    	_heartbeatScheduler.shutdown();
    	_executor.shutdown();
        return true;
    }
    
    @Override
    public String getName() {
    	return _name;
    }
}
  