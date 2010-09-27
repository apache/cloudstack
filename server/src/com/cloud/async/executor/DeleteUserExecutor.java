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

import com.cloud.api.BaseCmd;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobResult;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.event.EventTypes;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ManagementServer;
import com.cloud.user.User;
import com.google.gson.Gson;

public class DeleteUserExecutor extends BaseAsyncJobExecutor {
    public static final Logger s_logger = Logger.getLogger(DeleteUserExecutor.class.getName());
	
	public boolean execute() {
		Gson gson = GsonHelper.getBuilder().create();
		AsyncJobManager asyncMgr = getAsyncJobMgr();
		AsyncJobVO job = getJob();
		ManagementServer managementServer = asyncMgr.getExecutorContext().getManagementServer();
		DeleteUserParam param = gson.fromJson(job.getCmdInfo(), DeleteUserParam.class);
		
		try {
		    User user = managementServer.getUser(param.getUserId(), true);
		    String description = "Starting deleting User " + user.getUsername() + " (id: " + param.getUserId()
            + ") for accountId = " + user.getAccountId();		    
		    managementServer.saveStartedEvent(1L, 1L, EventTypes.EVENT_USER_DELETE, description, param.getEventId());
		    
		    if(managementServer.deleteUser(param.getUserId())) {
				asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, 
					"success");
			} else {
				asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, 
					"operation failed");
			}
		} catch(Exception e) {
			s_logger.warn("Unable to delete User " + param.getUserId() + ": " + e.getMessage(), e);
			asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, 
				e.getMessage());
		}
		return true;
	}
}
