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

package com.cloud.async.executor;

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobResult;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.async.SyncQueueItemVO;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ManagementServer;
import com.cloud.user.Account;
import com.cloud.user.UserVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.vm.DomainRouterVO;
import com.google.gson.Gson;

public class DisableUserExecutor extends BaseAsyncJobExecutor {
    public static final Logger s_logger = Logger.getLogger(DisableUserExecutor.class.getName());
	
	public boolean execute() {
		Gson gson = GsonHelper.getBuilder().create();
		AsyncJobManager asyncMgr = getAsyncJobMgr();
		AsyncJobVO job = getJob();
		ManagementServer managementServer = asyncMgr.getExecutorContext().getManagementServer();
		Long param = gson.fromJson(job.getCmdInfo(), Long.class);
		
		SyncQueueItemVO syncItem = getSyncSource();
		if(syncItem == null) {
			initialSchedule(managementServer, param.longValue());
		} else {
			if(allRouterOperationCeased(job)) {
				if(s_logger.isInfoEnabled())
					s_logger.info("All previous router operations have ceased, we can now disable account of the user " + param);
				
				UserVO user = asyncMgr.getExecutorContext().getUserDao().findById(param.longValue());
				if(user != null) {
					if(managementServer.disableAccount(user.getAccountId())) {
						asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, 
							"success");
					} else {
						asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, 
							"failed");
					}
				} else {
					if(s_logger.isInfoEnabled())
						s_logger.info("User " + param + " no longer exists, assuming it is already disbled");
					asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, 
							"success");
				}
			} else {
				if(s_logger.isInfoEnabled())
					s_logger.info("Previous operation on router " + syncItem.getContentId() 
						+ " has ceased, still more to go to disable account for user " + param);
			}
		}
		return true;
	}
	
	public void initialSchedule(ManagementServer managementServer, long userId) {
		AsyncJobManager asyncMgr = getAsyncJobMgr();
		UserVO user = asyncMgr.getExecutorContext().getUserDao().findById(userId);
		if(user == null) {
			if(s_logger.isInfoEnabled())
				s_logger.info("User " + userId + " does not exist");
			
			asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, 
				"User " + userId + " does not exist");
			return;
		}
		
		if(managementServer.disableUser(userId)) {
			if(needToDisableAccount(user)) {
				if(s_logger.isInfoEnabled())
					s_logger.info("This is the only user " + userId + " left for the account " + user.getAccountId() + ", we will disable account as well");
				
		        List<DomainRouterVO> routers = asyncMgr.getExecutorContext().getRouterDao().listBy(user.getAccountId());
		        if(routers.size() > 0) {
		        	scheduleOperationAfterAllRouterOperations(managementServer, user.getAccountId(), routers);
		        } else {
					if(s_logger.isInfoEnabled())
						s_logger.info("Account " + user.getAccountId() + " does not have DomR to serialize with, disable it directly");
		        	
		        	// no router being created under the account, disable the account directly
					if(managementServer.disableAccount(user.getAccountId())) {
						asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, 
							"success");
					} else {
						asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, 
							"failed");
					}
		        }
			}
		} else {
			if(s_logger.isInfoEnabled())
				s_logger.info("Unable to disable user " + userId);
			
			asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, 
					"Unable to disable user " + userId);
		}
	}
	
	private boolean needToDisableAccount(UserVO user) {
		AsyncJobManager asyncMgr = getAsyncJobMgr();
		
        List<UserVO> allUsersByAccount = asyncMgr.getExecutorContext().getUserDao().listByAccount(user.getAccountId());
        for (UserVO oneUser : allUsersByAccount) {
            if (oneUser.getState().equals(Account.ACCOUNT_STATE_ENABLED)) {
                return false;
            }
        }
        return true;
	}
	
	@DB
	protected void scheduleOperationAfterAllRouterOperations(ManagementServer managementServer, long accountId, 
		List<DomainRouterVO> routers) {
		
		AsyncJobManager asyncMgr = getAsyncJobMgr();
		AsyncJobVO job = getJob();
		
		Transaction txn = Transaction.currentTxn();
		try {
			txn.start();
			
			asyncMgr.updateAsyncJobStatus(job.getId(), routers.size(), "");
			for(DomainRouterVO router : routers) {
				if(s_logger.isInfoEnabled())
					s_logger.info("Serialize DisableUser operation with previous activities on router " + router.getId());
				asyncMgr.syncAsyncJobExecution(job.getId(), "Router", router.getId());
			}
			
			txn.commit();
		} catch (Exception e) {
			txn.rollback();
			s_logger.warn("Unexpected exception " + e.getMessage(), e);
			asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, 
				e.getMessage());
		} 
	}
	
	@DB
	protected boolean allRouterOperationCeased(AsyncJobVO job) {
		AsyncJobManager asyncMgr = getAsyncJobMgr();
		Transaction txn = Transaction.currentTxn();
		
		try {
			txn.start();
			
			AsyncJobVO jobUpdate = asyncMgr.getExecutorContext().getJobDao().lock(job.getId(), true);
			int progress = jobUpdate.getProcessStatus();
			jobUpdate.setProcessStatus(progress -1);
			asyncMgr.getExecutorContext().getJobDao().update(job.getId(), jobUpdate);
			
			txn.commit();
			
			return progress == 1;
		} catch(Exception e) {
			s_logger.warn("Unexpected exception " + e.getMessage(), e);
			
			txn.rollback();
		}
		return false;
	}
}
