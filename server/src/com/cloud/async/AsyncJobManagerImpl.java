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

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Type;
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

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import com.cloud.api.ApiDispatcher;
import com.cloud.api.ApiGsonHelper;
import com.cloud.api.ApiSerializerHelper;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.api.commands.QueryAsyncJobResultCmd;
import com.cloud.api.response.ExceptionResponse;
import com.cloud.async.dao.AsyncJobDao;
import com.cloud.cluster.ClusterManager;
import com.cloud.cluster.ClusterManagerListener;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.StackMaid;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExceptionUtil;
import com.cloud.utils.mgmt.JmxUtil;
import com.cloud.utils.net.MacAddress;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@Local(value={AsyncJobManager.class})
public class AsyncJobManagerImpl implements AsyncJobManager, ClusterManagerListener {
    public static final Logger s_logger = Logger.getLogger(AsyncJobManagerImpl.class.getName());
	private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 3; 	// 3 seconds
    
    private static final int MAX_ONETIME_SCHEDULE_SIZE = 50;
    private static final int HEARTBEAT_INTERVAL = 2000;
    private static final int GC_INTERVAL = 10000;				// 10 seconds
    
    private String _name;
    
    private AsyncJobExecutorContext _context;
    private SyncQueueManager _queueMgr;
    private ClusterManager _clusterMgr;
    private AccountManager _accountMgr;
    private AccountDao _accountDao;
    private AsyncJobDao _jobDao;
    private long _jobExpireSeconds = 86400;						// 1 day
    private long _jobCancelThresholdSeconds = 3600;             // 1 hour
    private ApiDispatcher _dispatcher;

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
    public List<AsyncJobVO> findInstancePendingAsyncJobs(AsyncJob.Type instanceType, Long accountId) {
    	return _jobDao.findInstancePendingAsyncJobs(instanceType, accountId);
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

    @Override @DB
    public void completeAsyncJob(long jobId, int jobStatus, int resultCode, Object resultObject) {
    	if(s_logger.isDebugEnabled()) {
            s_logger.debug("Complete async job-" + jobId + ", jobStatus: " + jobStatus +
    			", resultCode: " + resultCode + ", result: " + resultObject);
        }
    	
    	Transaction txt = Transaction.currentTxn();
    	try {
    		txt.start();
    		AsyncJobVO job = _jobDao.findById(jobId);
    		if(job == null) {
    	    	if(s_logger.isDebugEnabled()) {
                    s_logger.debug("job-" + jobId + " no longer exists, we just log completion info here. " + jobStatus +
    	    			", resultCode: " + resultCode + ", result: " + resultObject);
                }
    			
    			txt.rollback();
    			return;
    		}

    		job.setCompleteMsid(getMsid());
    		job.setStatus(jobStatus);
    		job.setResultCode(resultCode);

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
    		s_logger.error("Unexpected exception while completing async job-" + jobId, e);
    		txt.rollback();
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
                job.setResult(ApiSerializerHelper.toSerializedStringOld(resultObject));
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
	    	//job.setInstanceType(instanceType);
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
    public void syncAsyncJobExecution(AsyncJob job, String syncObjType, long syncObjId) {
    	// This method is re-entrant.  If an API developer wants to synchronized on an object, e.g. the router,
    	// when executing business logic, they will call this method (actually a method in BaseAsyncCmd that calls this).
    	// This method will get called every time their business logic executes.  The first time it exectues for a job
    	// there will be no sync source, but on subsequent execution there will be a sync souce.  If this is the first
    	// time the job executes we queue the job, otherwise we just return so that the business logic can execute.
        if (job.getSyncSource() != null) {
            return;
        }
    	
        if(s_logger.isDebugEnabled()) {
            s_logger.debug("Sync job-" + job.getId() + " execution on object " + syncObjType + "." + syncObjId);
        }

    	SyncQueueVO queue = null;

		// to deal with temporary DB exceptions like DB deadlock/Lock-wait time out cased rollbacks
    	// we retry five times until we throw an exception
		Random random = new Random();

    	for(int i = 0; i < 5; i++) {
    		queue = _queueMgr.queue(syncObjType, syncObjId, "AsyncJob", job.getId());
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
		    throw new AsyncCommandQueued(queue, "job-" + job.getId() + " queued");
		}
    }
    
    @Override
    public AsyncJob queryAsyncJobResult(QueryAsyncJobResultCmd cmd) {
        Account caller = UserContext.current().getCaller();

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

    @Override @DB
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
    			jobResult.setCmdOriginator(job.getCmdOriginator());
    			jobResult.setJobStatus(job.getStatus());
    			jobResult.setProcessStatus(job.getProcessStatus());
    			jobResult.setResult(job.getResult());
    			jobResult.setResultCode(job.getResultCode());
    			jobResult.setUuid(job.getUuid());
    			
    			if(job.getStatus() == AsyncJobResult.STATUS_SUCCEEDED ||
    				job.getStatus() == AsyncJobResult.STATUS_FAILED) {
    				
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
                    	s_logger.warn("Unable to register active job " + job.getId() + " to JMX monitoring due to exception " + ExceptionUtil.toString(e));
                    }
    
                    BaseAsyncCmd cmdObj = null;
                    Transaction txn = Transaction.open(Transaction.CLOUD_DB);
                    try {
                        jobId = job.getId();
                        NDC.push("job-" + jobId);
    
                        if(s_logger.isDebugEnabled()) {
                            s_logger.debug("Executing " + job.getCmd() + " for job-" + jobId);
                        }
    
                        Class<?> cmdClass = Class.forName(job.getCmd());
                        cmdObj = (BaseAsyncCmd)cmdClass.newInstance();
                        cmdObj.setJob(job);
    
                        Type mapType = new TypeToken<Map<String, String>>() {}.getType();
                        Gson gson = ApiGsonHelper.getBuilder().create();
                        Map<String, String> params = gson.fromJson(job.getCmdInfo(), mapType);
    
                        // whenever we deserialize, the UserContext needs to be updated
                        String userIdStr = params.get("ctxUserId");
                        String acctIdStr = params.get("ctxAccountId");
                        Long userId = null;
                        Account accountObject = null;
    
                        if (userIdStr != null) {
                            userId = Long.parseLong(userIdStr);
                        }
    
                        if (acctIdStr != null) {
                            accountObject = _accountDao.findById(Long.parseLong(acctIdStr));
                        }
    
                        UserContext.registerContext(userId, accountObject, null, false);
                        try {
                            // dispatch could ultimately queue the job
                            _dispatcher.dispatch(cmdObj, params);
        
                            // serialize this to the async job table
                            completeAsyncJob(jobId, AsyncJobResult.STATUS_SUCCEEDED, 0, cmdObj.getResponseObject());
                        } finally {
                            UserContext.unregisterContext();
                        }
    
                        // commands might need to be queued as part of synchronization here, so they just have to be re-dispatched from the queue mechanism...
                        if (job.getSyncSource() != null) {
                            _queueMgr.purgeItem(job.getSyncSource().getId());
                            checkQueue(job.getSyncSource().getQueueId());
                        }
    
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Done executing " + job.getCmd() + " for job-" + jobId);
                        }
                        
                    } catch(Throwable e) {
                        if (e instanceof AsyncCommandQueued) {
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("job " + job.getCmd() + " for job-" + jobId + " was queued, processing the queue.");
                            }
                            checkQueue(((AsyncCommandQueued)e).getQueue().getId());
                        } else {
                            String errorMsg = null;
                            int errorCode = BaseCmd.INTERNAL_ERROR;
                            if (!(e instanceof ServerApiException)) {
                                s_logger.error("Unexpected exception while executing " + job.getCmd(), e);
                                errorMsg = e.getMessage();
                            } else {
                                ServerApiException sApiEx = (ServerApiException)e;
                                errorMsg = sApiEx.getDescription();
                                errorCode = sApiEx.getErrorCode();
                            }
    
                            ExceptionResponse response = new ExceptionResponse();
                            response.setErrorCode(errorCode);
                            response.setErrorText(errorMsg);
                            response.setResponseName((cmdObj == null) ? "unknowncommandresponse" : cmdObj.getCommandName());
    
                            // FIXME:  setting resultCode to BaseCmd.INTERNAL_ERROR is not right, usually executors have their exception handling
                            //         and we need to preserve that as much as possible here
                            completeAsyncJob(jobId, AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, response);
    
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
                        	s_logger.warn("Unable to unregister active job " + job.getId() + " from JMX monitoring");
                        }
                    	
                        StackMaid.current().exitCleanup();
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
        AsyncJobVO job = _jobDao.findById(item.getContentId());
        if (job != null) {
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("Schedule queued job-" + job.getId());
            }

            job.setFromPreviousSession(fromPreviousSession);
            job.setSyncSource(item);
            
            job.setCompleteMsid(getMsid());
            _jobDao.update(job.getId(), job);
            
            try {
            	scheduleExecution(job);
			} catch(RejectedExecutionException e) {
				s_logger.warn("Execution for job-" + job.getId() + " is rejected, return it to the queue for next turn");
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
                s_logger.debug("Release sync source for job-" + executor.getJob().getId() + " sync source: "
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
				} finally {
					StackMaid.current().exitCleanup();
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
			
			private void reallyRun() {
				try {
					s_logger.trace("Begin cleanup expired async-jobs");
					
					Date cutTime = new Date(DateUtil.currentGMTTime().getTime() - _jobExpireSeconds*1000);
					
					// limit to 100 jobs per turn, this gives cleanup throughput as 600 jobs per minute
					// hopefully this will be fast enough to balance potential growth of job table
					List<AsyncJobVO> l = _jobDao.getExpiredJobs(cutTime, 100);
					if(l != null && l.size() > 0) {
						for(AsyncJobVO job : l) {
							_jobDao.expunge(job.getId());
						}
					}
					
					// forcely cancel blocking queue items if they've been staying there for too long
				    List<SyncQueueItemVO> blockItems = _queueMgr.getBlockedQueueItems(_jobCancelThresholdSeconds*1000, false);
				    if(blockItems != null && blockItems.size() > 0) {
				        for(SyncQueueItemVO item : blockItems) {
				            if(item.getContentType().equalsIgnoreCase("AsyncJob")) {
                                completeAsyncJob(item.getContentId(), AsyncJobResult.STATUS_FAILED, 0, getResetResultResponse("Job is cancelled as it has been blocking others for too long"));
                            }
				        
				            // purge the item and resume queue processing
				            _queueMgr.purgeItem(item.getId());
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
				if(contentType != null && contentType.equals("AsyncJob")) {
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
    	_name = name;
    
		ComponentLocator locator = ComponentLocator.getCurrentLocator();
		
		ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
		if (configDao == null) {
			throw new ConfigurationException("Unable to get the configuration dao.");
		}

		int expireMinutes = NumbersUtil.parseInt(
		       configDao.getValue(Config.JobExpireMinutes.key()), 24*60);
		_jobExpireSeconds = (long)expireMinutes*60;
		
		_jobCancelThresholdSeconds = NumbersUtil.parseInt(
		       configDao.getValue(Config.JobCancelThresholdMinutes.key()), 60);
		_jobCancelThresholdSeconds *= 60;

		_accountDao = locator.getDao(AccountDao.class);
		if (_accountDao == null) {
            throw new ConfigurationException("Unable to get " + AccountDao.class.getName());
		}
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
		
		_accountMgr = locator.getManager(AccountManager.class);

		_dispatcher = ApiDispatcher.getInstance();
		

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
        		_queueMgr.resetQueueProcess(msHost.getId());
        		_jobDao.resetJobProcess(msHost.getId(), BaseCmd.INTERNAL_ERROR, getSerializedErrorMessage("job cancelled because of management server restart"));
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
    		_queueMgr.resetQueueProcess(getMsid());
    		_jobDao.resetJobProcess(getMsid(), BaseCmd.INTERNAL_ERROR, getSerializedErrorMessage("job cancelled because of management server restart"));
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
		resultObject.setErrorCode(BaseCmd.INTERNAL_ERROR);
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
    
    @Override
    public String getName() {
    	return _name;
    }
}
