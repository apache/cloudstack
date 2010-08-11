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

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.api.BaseCmd;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobResult;
import com.cloud.async.AsyncJobVO;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.exception.InternalErrorException;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ManagementServer;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.vm.DomainRouter;
import com.google.gson.Gson;

public class StartRouterExecutor extends VMOperationExecutor {
    public static final Logger s_logger = Logger.getLogger(StartRouterExecutor.class.getName());

	public boolean execute() {
    	Gson gson = GsonHelper.getBuilder().create();
    	AsyncJobManager asyncMgr = getAsyncJobMgr();
    	AsyncJobVO job = getJob();
		ManagementServer managementServer = asyncMgr.getExecutorContext().getManagementServer();
    	VMOperationParam param = gson.fromJson(job.getCmdInfo(), VMOperationParam.class);
    	
		if(getSyncSource() == null) {
	    	asyncMgr.syncAsyncJobExecution(job.getId(), "Router", param.getVmId());
			return true;
		} else {
		    boolean success = false;
	    	try {
	    		DomainRouter router = managementServer.startRouter(param.getVmId(), param.getEventId());
	    		if(router != null){
	    		    success = true;
	    			asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, 
						RouterExecutorHelper.composeResultObject(managementServer, router));
	    		}
	    		else
	    			asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, 
						"operation failed");
			} catch (InternalErrorException e) {
				asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, 
					e.getMessage());
			} catch(Exception e) {
				s_logger.warn("Unable to start router " + param.getVmId() + ":" + e.getMessage(), e);
				asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, 
					e.getMessage());
			} finally {
			    if(!success){
                    // Save completed event when operation fails
                    asyncMgr.getExecutorContext().getManagementServer().saveEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventVO.LEVEL_ERROR, EventTypes.EVENT_ROUTER_START, "Failed to start router", null, param.getEventId());
			    }
			}
	    	return true;
		}
	}
	
	public void processAnswer(VMOperationListener listener, long agentId, long seq, Answer answer) {
	}
	
	public void processDisconnect(VMOperationListener listener, long agentId) {
	}

	public void processTimeout(VMOperationListener listener, long agentId, long seq) {
	}
}
