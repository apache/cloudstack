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

import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ManagementServer;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Snapshot.Type;
import com.cloud.user.Account;
import com.google.gson.Gson;

public class CreateSnapshotExecutor extends BaseAsyncJobExecutor {
    public static final Logger s_logger = Logger.getLogger(CreateSnapshotExecutor.class.getName());
    
	public boolean execute() {
    	AsyncJobManager asyncMgr = getAsyncJobMgr();
    	AsyncJobVO job = getJob();
    	Gson gson = GsonHelper.getBuilder().create();
    	
    	/*
		if (getSyncSource() == null) {
	    	SnapshotOperationParam param = gson.fromJson(job.getCmdInfo(), SnapshotOperationParam.class);
	    	asyncMgr.syncAsyncJobExecution(job.getId(), "Volume", param.getVolumeId());
	    	
	    	// always true if it does not have sync-source
	    	return true;
		} else {
	    	SnapshotOperationParam param = gson.fromJson(job.getCmdInfo(), SnapshotOperationParam.class);
	    	SnapshotManager snapshotManager = asyncMgr.getExecutorContext().getSnapshotMgr();
	    	ManagementServer managementServer = getAsyncJobMgr().getExecutorContext().getManagementServer();
	    	VolumeDao volumeDao = asyncMgr.getExecutorContext().getVolumeDao();
	    	long volumeId = param.getVolumeId();
	    	long policyId = param.getPolicyId();
	    	long startEventId = param.getEventId();
	    	
	    	long snapshotId = 0;
	    	long userId = param.getUserId();
	    	
	    	// By default assume that everything has failed.
            boolean backedUp = false;
	    	Long jobId = getJob().getId();
            int result = AsyncJobResult.STATUS_FAILED;
            int errorCode = BaseCmd.INTERNAL_ERROR;
            Object resultObject = "Failed to create snapshot.";
            VolumeVO vol = null;
            
	    	try {
	    		vol = volumeDao.acquire(volumeId, 10);
	    		if( vol != null) {
	    		    managementServer.saveStartedEvent(userId, vol.getAccountId(), EventTypes.EVENT_SNAPSHOT_CREATE, "Start creating snapshot for volume:"+volumeId, startEventId);
		    	    SnapshotVO snapshot = snapshotManager.createSnapshot(userId, volumeId, policyId);
	
			    	if (snapshot != null && snapshot.getStatus() == Snapshot.Status.CreatedOnPrimary) {
					    snapshotId = snapshot.getId();
					    asyncMgr.updateAsyncJobStatus(jobId, BaseCmd.PROGRESS_INSTANCE_CREATED, snapshotId);
					    backedUp = snapshotManager.backupSnapshotToSecondaryStorage(userId, snapshot, startEventId);
					    if (backedUp) {
					        result = AsyncJobResult.STATUS_SUCCEEDED;
					        errorCode = 0; // Success
					        resultObject = composeResultObject(snapshot);
					    }
					    else {
					        // More specific error
					        resultObject = "Created snapshot: " + snapshotId + " on primary but failed to backup on secondary";
					    }
					} else if (snapshot != null && snapshot.getStatus() == Snapshot.Status.EmptySnapshot) {
					    resultObject ="There is no change since last snapshot, please use last snapshot";
		                   s_logger.warn(resultObject);
					}
	    		} else {
	    			resultObject = "Another snapshot is being created for " + volumeId + " try another time ";
		    		s_logger.warn(resultObject);
	    		}
			} catch(Exception e) {
	    	    resultObject = "Unable to create snapshot: " + e.getMessage();
	    		s_logger.warn(resultObject, e);
	    	} finally {
	    		if( vol != null ){
	    			volumeDao.release(volumeId);
	    		}
	    	}

			// In all cases, ensure that we call completeAsyncJob to the asyncMgr.
	    	asyncMgr.completeAsyncJob(jobId, result, errorCode, resultObject);
	    	
	    	// Cleanup jobs to do after the snapshot has been created.
	    	snapshotManager.postCreateSnapshot(userId, volumeId, snapshotId, policyId, backedUp);
	    	return true;
		}
		*/
    	return true;
	}
	
	private CreateSnapshotResultObject composeResultObject(Snapshot snapshot) {
		CreateSnapshotResultObject resultObject = new CreateSnapshotResultObject();
		ManagementServer managementServer = getAsyncJobMgr().getExecutorContext().getManagementServer();
//		VolumeVO volume = managementServer.findVolumeById(snapshot.getVolumeId());
		
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
//				resultObject.setDomainName(getAsyncJobMgr().getExecutorContext().getManagementServer().findDomainIdById(domainId).getName());
			}
			
		}
		String snapshotTypeStr = Type.values()[snapshot.getSnapshotType()].name();
		resultObject.setSnapshotType(snapshotTypeStr);
		resultObject.setVolumeId(snapshot.getVolumeId());
//		resultObject.setVolumeName(volume.getName());
//		resultObject.setVolumeType(volume.getVolumeType());
		resultObject.setCreated(snapshot.getCreated());
		resultObject.setName(snapshot.getName());
		return resultObject;
	}
}
