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
import com.cloud.dc.ClusterVO;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ManagementServer;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.StorageStats;
import com.google.gson.Gson;

public class PreparePrimaryStorageMaintenanceExecutor extends BaseAsyncJobExecutor {
    public static final Logger s_logger = Logger.getLogger(PreparePrimaryStorageMaintenanceExecutor.class.getName());
	
	public boolean execute() {
		Gson gson = GsonHelper.getBuilder().create();
		AsyncJobManager asyncMgr = getAsyncJobMgr();
		AsyncJobVO job = getJob();
		ManagementServer managementServer = asyncMgr.getExecutorContext().getManagementServer();
		Long param = gson.fromJson(job.getCmdInfo(), Long.class);
		Long userId = job.getUserId();
		/*
		try {
			boolean result = managementServer.preparePrimaryStorageForMaintenance(param.longValue(), userId.longValue());
			if(result)
			{
				StoragePoolVO primaryStorage = managementServer.findPoolById(param);
				
				asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, 
						composeResultObject(primaryStorage,managementServer));
			}
			else
			{
				StoragePoolVO primaryStorage = managementServer.findPoolById(param);

				asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, 
						composeResultObject(primaryStorage,managementServer));
			}
		} catch(Exception e) {
			s_logger.warn("Unable to prepare maintenance: " + e.getMessage(), e);
			asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, 
					e.getMessage());
		}
		*/
		return true;
	}
	
		private PrimaryStorageResultObject composeResultObject(StoragePoolVO storagePoolVO, ManagementServer managementServer)
	    {

	    	PrimaryStorageResultObject primaryStorageRO = new PrimaryStorageResultObject();

	    	primaryStorageRO.setId(storagePoolVO.getId());
	    	
	    	primaryStorageRO.setName(storagePoolVO.getName());
	    	primaryStorageRO.setType(storagePoolVO.getPoolType().toString());
	    	primaryStorageRO.setState(storagePoolVO.getStatus().toString());
	    	primaryStorageRO.setIpAddress(storagePoolVO.getHostAddress());
            primaryStorageRO.setZoneId(storagePoolVO.getDataCenterId());
//            primaryStorageRO.setZoneName(managementServer.getDataCenterBy(storagePoolVO.getDataCenterId()).getName());

            if (storagePoolVO.getPodId() != null && managementServer.findHostPodById(storagePoolVO.getPodId()) != null) {
            	primaryStorageRO.setPodId(storagePoolVO.getPodId());
            	primaryStorageRO.setPodName((managementServer.findHostPodById(storagePoolVO.getPodId())).getName());
            }
            
            if (storagePoolVO.getCreated() != null) {
                primaryStorageRO.setCreated(storagePoolVO.getCreated());
            }
            primaryStorageRO.setDiskSizeTotal(storagePoolVO.getCapacityBytes());
            
//            StorageStats stats = managementServer.getStoragePoolStatistics(storagePoolVO.getId());
            long capacity = storagePoolVO.getCapacityBytes();
            long available = storagePoolVO.getAvailableBytes() ;
            long used = capacity - available;

//            if (stats != null) {
//                used = stats.getByteUsed();
//                available = capacity - used;
//            }
            
            primaryStorageRO.setDiskSizeAllocated(used);
            if (storagePoolVO.getClusterId() != null) 
            {
//            	ClusterVO cluster = managementServer.findClusterById(storagePoolVO.getClusterId());
            	primaryStorageRO.setClusterId(storagePoolVO.getClusterId());
//            	primaryStorageRO.setClusterName(cluster.getName());
            }
            
//            primaryStorageRO.setTags(managementServer.getStoragePoolTags(storagePoolVO.getId()));
            return primaryStorageRO;

	    }
}
