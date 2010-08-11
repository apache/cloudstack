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

package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.async.AsyncJobResult;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.executor.CreateSnapshotResultObject;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.serializer.SerializerHelper;
import com.cloud.server.ManagementServer;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VolumeVO;
import com.cloud.utils.Pair;

public class CreateSnapshotCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(CreateSnapshotCmd.class.getName());
	private static final String s_name = "createsnapshotresponse";
	private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.VOLUME_ID, Boolean.TRUE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
    }

    public String getName() {
        return s_name;
    }
    
    public static String getResultObjectName() {
    	return "snapshot";
    }
    
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }
	
    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Long volumeId = (Long) params.get(BaseCmd.Properties.VOLUME_ID.getName());
        Long userId = (Long) params.get(BaseCmd.Properties.USER_ID.getName());

        ManagementServer managementServer = getManagementServer();
        // Verify that a volume exists with a specified volume ID
        VolumeVO volume = managementServer.findVolumeById(volumeId);
        if (volume == null) {
            throw new ServerApiException (BaseCmd.PARAM_ERROR, "Unable to find a volume with id " + volumeId);
        }
        
        // If an account was passed in, make sure that it matches the account of the volume
        checkAccountPermissions(params, volume.getAccountId(), volume.getDomainId(), "volume", volumeId);

        // If command is executed via 8096 port, set userId to the id of System account (1)
        if (userId == null) {
            userId = Long.valueOf(1);
        }

        try {
            long jobId = managementServer.createSnapshotAsync(userId, volumeId.longValue());
            if (jobId == 0) {
            	s_logger.warn("Unable to schedule async-job for CreateSnapshot comamnd");
            } else {
    	        if (s_logger.isDebugEnabled())
    	        	s_logger.debug("CreateSnapshot command has been accepted, job id: " + jobId);
            }
            
            long snapshotId = waitInstanceCreation(jobId);
            
            List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.SNAPSHOT_ID.getName(), Long.valueOf(snapshotId))); 
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), Long.valueOf(jobId))); 
            
            return returnValues;
        } catch (ResourceAllocationException rae) {
        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Unable to create a snapshot for volume with id " + volumeId + ": " + rae.getMessage());
        } catch (ServerApiException apiEx) {
            throw apiEx;
        } catch (Exception ex) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Unable to create a snapshot for volume with id " + volumeId + " because of: " + ex.getMessage());
        } 
    }
    
    protected long waitInstanceCreation(long jobId) {
        ManagementServer mgr = getManagementServer();

        long snapshotId = 0;
        AsyncJobVO job = null;
        boolean interruped = false;
        
        // as job may be executed in other management server, we need to do a database polling here
        try {
        	boolean quit = false;
	        while(!quit) {
	        	job = mgr.findAsyncJobById(jobId);
	        	if(job == null) {
	        		s_logger.error("CreateSnapshotAsync.waitInstanceCreation error: job-" + jobId + " no longer exists");
	        		break;
	        	}
	        	
	        	switch(job.getStatus()) {
	        	case AsyncJobResult.STATUS_IN_PROGRESS :
	        		if(job.getProcessStatus() == BaseCmd.PROGRESS_INSTANCE_CREATED) {
	        			Long id = (Long)SerializerHelper.fromSerializedString(job.getResult());
	        			if(id != null) {
	        				snapshotId = id.longValue();
	        				if(s_logger.isDebugEnabled())
	        					s_logger.debug("CreateSnapshotAsync succeeded in taking snapshot on primary, snapshotId: " + snapshotId);
	        			} else {
	        				s_logger.warn("CreateSnapshotAsync succeeded in taking snapshot on primary, but value as null?");
	        			}
	        			quit = true;
	        		}
	        		break;
	        		
	        	case AsyncJobResult.STATUS_SUCCEEDED :
	        		{
	        		    CreateSnapshotResultObject resultObject = (CreateSnapshotResultObject)SerializerHelper.fromSerializedString(job.getResult());
	        			if(resultObject != null) {
	        				snapshotId = resultObject.getId();
	        				
	        				if(s_logger.isDebugEnabled())
	        					s_logger.debug("CreateSnapshotAsync succeeded in backing up snapshot to secondary, snapshotId: " + snapshotId);
	        			} else {
	        				s_logger.warn("CreateSnapshotAsync successfully completed, but result object is null?");
	        			}
	        		}
	        		quit = true;
	        		break;
	        		
	        	case AsyncJobResult.STATUS_FAILED :
        			s_logger.error("CreateSnapshotAsync job-" + jobId + " failed, result: " + job.getResult());
	        		quit = true;
	        		break;
	        	}
	        	
	        	if(quit)
	        		break;
	        	
	        	try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					interruped = true;
				}
	        }
        } finally {
	        if(interruped)
	        	Thread.currentThread().interrupt();
        }
        return snapshotId;
	}

    protected long getInstanceIdFromJobSuccessResult(String result) {
    	CreateSnapshotResultObject resultObject = (CreateSnapshotResultObject)SerializerHelper.fromSerializedString(result);
    	if(resultObject != null) {
    		return resultObject.getId();
    	}
    	return 0;
    }
}
