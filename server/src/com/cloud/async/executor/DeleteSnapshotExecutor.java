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
import com.cloud.storage.Snapshot;
import com.cloud.storage.snapshot.SnapshotManager;
import com.google.gson.Gson;

public class DeleteSnapshotExecutor extends BaseAsyncJobExecutor {
    public static final Logger s_logger = Logger.getLogger(DeleteSnapshotExecutor.class.getName());

	public boolean execute() {
		
    	Gson gson = GsonHelper.getBuilder().create();
    	AsyncJobManager asyncMgr = getAsyncJobMgr();
    	AsyncJobVO job = getJob();
    	SnapshotManager snapshotManager = asyncMgr.getExecutorContext().getSnapshotMgr();
    	ManagementServer managementServer = asyncMgr.getExecutorContext().getManagementServer();
    	
		if(getSyncSource() == null) {
	    	SnapshotOperationParam param = gson.fromJson(job.getCmdInfo(), SnapshotOperationParam.class);
	    	Snapshot snapshot = managementServer.findSnapshotById(param.getSnapshotId());
	    	if(snapshot != null) {
		    	asyncMgr.syncAsyncJobExecution(job.getId(), "Volume", snapshot.getVolumeId());
	    	} else {
				asyncMgr.completeAsyncJob(getJob().getId(), 
		    		AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, "snapshot no long exists");
	    	}
	    	
	    	// always true if it does not have sync-source
	    	return true;
		} else {
	    	SnapshotOperationParam param = gson.fromJson(job.getCmdInfo(), SnapshotOperationParam.class);
	    	Long jobId = getJob().getId();
            int result = AsyncJobResult.STATUS_FAILED;
            int errorCode = BaseCmd.INTERNAL_ERROR;
            Object resultObject = "Failed to delete snapshot.";
            
	    	try {
				if(snapshotManager.deleteSnapshot(param.getUserId(), param.getSnapshotId(), param.getPolicyId())) {
				    result = AsyncJobResult.STATUS_SUCCEEDED; 
				    errorCode = 0;
				    resultObject = "success"; // Might want to put something more substantial later.
				}
	    	} catch(Exception e) {
	    	    resultObject = "Unable to destroy snapshot: " + e.getMessage();
	    		s_logger.warn(resultObject, e);
	    	}
	    	// In all cases, ensure that we call completeAsyncJob to the asyncMgr.
            asyncMgr.completeAsyncJob(jobId, result, errorCode, resultObject);
            return true;
		}
	}
    
}
