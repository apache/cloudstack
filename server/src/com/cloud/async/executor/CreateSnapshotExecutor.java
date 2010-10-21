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
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ManagementServer;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.Snapshot.SnapshotType;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.user.Account;
import com.google.gson.Gson;

public class CreateSnapshotExecutor extends BaseAsyncJobExecutor {
    public static final Logger s_logger = Logger.getLogger(CreateSnapshotExecutor.class.getName());
    
	public boolean execute() {
    	AsyncJobManager asyncMgr = getAsyncJobMgr();
    	AsyncJobVO job = getJob();
    	Gson gson = GsonHelper.getBuilder().create();
    	
		if (getSyncSource() == null) {
	    	SnapshotOperationParam param = gson.fromJson(job.getCmdInfo(), SnapshotOperationParam.class);
	    	asyncMgr.syncAsyncJobExecution(job.getId(), "Volume", param.getVolumeId());
	    	
	    	// always true if it does not have sync-source
	    	return true;
		} else {
	    	SnapshotOperationParam param = gson.fromJson(job.getCmdInfo(), SnapshotOperationParam.class);
	    	SnapshotManager snapshotManager = asyncMgr.getExecutorContext().getSnapshotMgr();
	    	long volumeId = param.getVolumeId();
	    	List<Long> policyIds = param.getPolicyIds();
	    	long snapshotId = 0;
	    	long userId = param.getUserId();
	    	
	    	// By default assume that everything has failed.
            boolean backedUp = false;
	    	Long jobId = getJob().getId();
            int result = AsyncJobResult.STATUS_FAILED;
            int errorCode = BaseCmd.INTERNAL_ERROR;
            Object resultObject = "Failed to create snapshot.";
            
	    	try {
	    	    SnapshotVO snapshot = snapshotManager.createSnapshot(userId, param.getVolumeId(), param.getPolicyIds());
		    	if (snapshot != null) {
                    Snapshot.Status status = snapshot.getStatus();
				    snapshotId = snapshot.getId();
				    if( status == Snapshot.Status.CreatedOnPrimary ) {
    				    asyncMgr.updateAsyncJobStatus(jobId, BaseCmd.PROGRESS_INSTANCE_CREATED, snapshotId);
    				    backedUp = snapshotManager.backupSnapshotToSecondaryStorage(userId, snapshot);
				    } else if ( status == Snapshot.Status.BackedUp ){
				        backedUp = true;
				    } else {
                        resultObject = "Snapshot: " + snapshotId + " creation failed, the status is " + status.toString();
				    }
				    if (backedUp) {
				        result = AsyncJobResult.STATUS_SUCCEEDED;
				        errorCode = 0; // Success
				        resultObject = composeResultObject(snapshot);
				    } else {
				        // More specific error
				        resultObject = "Created snapshot: " + snapshotId + " on primary but failed to backup on secondary";
				    }
				}
			} catch(Exception e) {
	    	    resultObject = "Unable to create snapshot: " + e.getMessage();
	    		s_logger.warn(resultObject, e);
	    	}

			// In all cases, ensure that we call completeAsyncJob to the asyncMgr.
	    	asyncMgr.completeAsyncJob(jobId, result, errorCode, resultObject);
	    	
	    	// Cleanup jobs to do after the snapshot has been created.
	    	snapshotManager.postCreateSnapshot(userId, volumeId, snapshotId, policyIds, backedUp);
	    	return true;
		}
	}
	
	private CreateSnapshotResultObject composeResultObject(Snapshot snapshot) {
		CreateSnapshotResultObject resultObject = new CreateSnapshotResultObject();
		ManagementServer managementServer = getAsyncJobMgr().getExecutorContext().getManagementServer();
		VolumeVO volume = managementServer.findVolumeById(snapshot.getVolumeId());
		
		resultObject.setId(snapshot.getId());
		long domainId = -1;
		Account account = getAsyncJobMgr().getExecutorContext().getAccountDao().findById(snapshot.getAccountId());
		if(account != null)
		{
			resultObject.setAccountName(account.getAccountName());
			domainId = account.getDomainId();
			
			if(domainId != -1)
			{
				resultObject.setDomainId(domainId);
				resultObject.setDomainName(getAsyncJobMgr().getExecutorContext().getManagementServer().findDomainIdById(domainId).getName());
			}
			
		}
		String snapshotTypeStr = SnapshotType.values()[snapshot.getSnapshotType()].name();
		resultObject.setSnapshotType(snapshotTypeStr);
		resultObject.setVolumeId(snapshot.getVolumeId());
		resultObject.setVolumeName(volume.getName());
		resultObject.setVolumeType(volume.getVolumeType());
		resultObject.setCreated(snapshot.getCreated());
		resultObject.setName(snapshot.getName());
		return resultObject;
	}
}
