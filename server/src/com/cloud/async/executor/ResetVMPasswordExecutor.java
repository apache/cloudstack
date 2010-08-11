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
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ManagementServer;
import com.cloud.vm.UserVmVO;
import com.google.gson.Gson;

public class ResetVMPasswordExecutor extends BaseAsyncJobExecutor {
    public static final Logger s_logger = Logger.getLogger(ResetVMPasswordExecutor.class.getName());
	
	public boolean execute() {
    	AsyncJobManager asyncMgr = getAsyncJobMgr();
    	Gson gson = GsonHelper.getBuilder().create();
    	AsyncJobVO job = getJob();

		if(getSyncSource() == null) {
	    	VMOperationParam param = gson.fromJson(job.getCmdInfo(), VMOperationParam.class);
	    	asyncMgr.syncAsyncJobExecution(job.getId(), "UserVM", param.getVmId());
	    	
	    	// always true if it does not have sync-source
	    	return true;
		} else {
			ManagementServer managementServer = asyncMgr.getExecutorContext().getManagementServer();
			ResetVMPasswordParam param = gson.fromJson(job.getCmdInfo(), ResetVMPasswordParam.class);
			
			asyncMgr.updateAsyncJobAttachment(job.getId(), "vm_instance", param.getVmId());
			try {
				boolean success = managementServer.resetVMPassword(param.getUserId(), param.getVmId(), param.getPassword());
				if(success) {
			        UserVmVO userVm = managementServer.findUserVMInstanceById(param.getVmId());
					asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, 
						VMExecutorHelper.composeResultObject(managementServer, userVm, param.getPassword()));
				}
				else
					asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, 
						"Operation failed");
			} catch(Exception e) {
				s_logger.warn("Unable to reset password for VM " + param.getVmId() + ": " + e.getMessage(), e);
				asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, 
					e.getMessage());
			}
		}
		return true;
	}
}
