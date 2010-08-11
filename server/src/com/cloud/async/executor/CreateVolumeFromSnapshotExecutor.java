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
import com.cloud.async.AsyncInstanceCreateStatus;
import com.cloud.async.AsyncJobExecutorContext;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobResult;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ManagementServer;
import com.cloud.storage.Snapshot;
import com.cloud.storage.StorageManager;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.google.gson.Gson;

public class CreateVolumeFromSnapshotExecutor extends BaseAsyncJobExecutor {
    public static final Logger s_logger = Logger.getLogger(CreateVolumeFromSnapshotExecutor.class.getName());
    
	public boolean execute() {
    	AsyncJobManager asyncMgr = getAsyncJobMgr();
    	AsyncJobVO job = getJob();
    	Gson gson = GsonHelper.getBuilder().create();
    	AsyncJobExecutorContext context = asyncMgr.getExecutorContext();
    	ManagementServer managerServer = context.getManagementServer();
    	//SnapshotManager snapshotManager = context.getSnapshotMgr();
    	StorageManager storageManager = context.getStorageMgr();
    	AccountManager accountManager = context.getAccountMgr();
    	
		if (getSyncSource() == null) {
	    	SnapshotOperationParam param = gson.fromJson(job.getCmdInfo(), SnapshotOperationParam.class);
	    	asyncMgr.syncAsyncJobExecution(job.getId(), "Volume", param.getVolumeId());
	    	
	    	// always true if it does not have sync-source
	    	return true;
		} else {
	    	SnapshotOperationParam param = gson.fromJson(job.getCmdInfo(), SnapshotOperationParam.class);
	    	VolumeVO volume = null;
	    	
	    	// By default assume that everything has failed.
            Long jobId = getJob().getId();
            int result = AsyncJobResult.STATUS_FAILED;
            int errorCode = BaseCmd.INTERNAL_ERROR;
            Object resultObject = "Failed to create volume from snapshot: " + param.getSnapshotId();
            
	    	try {
	    	    long accountId = param.getAccountId();
	    	    long userId = param.getUserId();
	    	    long snapshotId = param.getSnapshotId();
	    	    String volumeName = param.getName();
	    	    Snapshot snapshot = managerServer.findSnapshotById(snapshotId);
	    	    if (snapshot == null) {
	    	        throw new InvalidParameterValueException("The snapshot was deleted after createVolumeFromSnapshot command was issued.");
	    	    }
	    	    AccountVO account = (AccountVO) managerServer.findAccountById(snapshot.getAccountId());
	    	    
	            // Check that the resource limit for volumes won't be exceeded
	            if (accountManager.resourceLimitExceeded(account, ResourceType.volume)) {
	                ResourceAllocationException rae = new ResourceAllocationException("Maximum number of volumes for account: " + account.getAccountName() + " has been exceeded.");
	                rae.setResourceType("volume");
	                throw rae;
	            }
	            
	            volume = storageManager.createVolumeFromSnapshot(userId, accountId, snapshotId, volumeName, param.getEventId());

		    	if (volume != null && volume.getStatus() == AsyncInstanceCreateStatus.Created) {
				    result = AsyncJobResult.STATUS_SUCCEEDED;
                    errorCode = 0; // Success
                    resultObject = composeResultObject(volume);
                    // Increment the number of volumes
                    accountManager.incrementResourceCount(accountId, ResourceType.volume);
				}
	    	} catch(Exception e) {
	    	    resultObject = (String)resultObject + ", reason: " + e.getMessage();
	    		s_logger.warn(resultObject, e);
	    	}
	    	// In all cases, ensure that we call completeAsyncJob to the asyncMgr.
            asyncMgr.completeAsyncJob(jobId, result, errorCode, resultObject);
			return true;
		}
	}

    private VolumeOperationResultObject composeResultObject(VolumeVO volume) 
    {
        VolumeOperationResultObject resultObject = new VolumeOperationResultObject();
        ManagementServer managementServer = getAsyncJobMgr().getExecutorContext().getManagementServer();
        resultObject.setId(volume.getId());
        resultObject.setName(volume.getName());
        resultObject.setVolumeType(volume.getVolumeType());
        resultObject.setVolumeSize(volume.getSize());
        resultObject.setDiskOfferingId(volume.getDiskOfferingId());
        resultObject.setCreatedDate(volume.getCreated());
        resultObject.setState(volume.getStatus());
        Long accountId = volume.getAccountId();
        Account account = managementServer.findAccountById(accountId);
        resultObject.setAccountName(account.getAccountName());
        resultObject.setDomainId(volume.getDomainId());
        resultObject.setDomain(managementServer.findDomainIdById(volume.getDomainId()).getName());
        resultObject.setZoneId(volume.getDataCenterId());
        resultObject.setZoneName(getAsyncJobMgr().getExecutorContext().getManagementServer().getDataCenterBy(volume.getDataCenterId()).getName());
        resultObject.setStorageType("shared"); // NOTE: You can never create a local disk volume but if that changes, we need to change this
        if (volume.getPoolId() != null)
            resultObject.setStorage(managementServer.findPoolById(volume.getPoolId()).getName());
        return resultObject;
    }
}
