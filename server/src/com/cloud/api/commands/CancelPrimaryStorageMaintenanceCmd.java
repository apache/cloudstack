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

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;

@Implementation(method="cancelPrimaryStorageMaintenance", manager=Manager.ManagementServer)
public class CancelPrimaryStorageMaintenanceCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(CancelPrimaryStorageMaintenanceCmd.class.getName());
	
    private static final String s_name = "cancelprimarystoragemaintenanceresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="id", type=CommandType.LONG, required=true)
    private Long id;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return s_name;
    }
    
    public static String getResultObjectName() {
    	return "primarystorage";
    }

    /*
    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Long storagePoolId = (Long)params.get(BaseCmd.Properties.ID.getName());
        Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        //verify input parameters
    	StoragePoolVO storagePool = getManagementServer().findPoolById(storagePoolId);
    	
    	if (storagePool == null) {
    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "Primary storage with id " + storagePoolId + " doesn't exist");
    	}
        
    	if (!storagePool.getStatus().equals(Status.Maintenance)) {
    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "Primary storage with id " + storagePoolId + " is not ready for migration, as the status is:"+storagePool.getStatus().toString());
    	}
    	
    	long jobId = 0;
    	try {
    		jobId = getManagementServer().cancelPrimaryStorageMaintenanceAsync(storagePoolId);
    	} catch (InvalidParameterValueException e) {
    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to cancel primary storage maintenance: " + e.getMessage());
    	}
    	
        if(jobId == 0) {
        	s_logger.warn("Unable to schedule async-job for CancelPrimaryStorageMaintenance command");
        } else {
	        if(s_logger.isDebugEnabled())
	        	s_logger.debug("CancelPrimaryStorageMaintenance command has been accepted, job id: " + jobId);
        }
        
        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), Long.valueOf(jobId))); 
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.STATE.getName(), Status.PrepareForMaintenance)); 
        return returnValues;
    }
    */
}
