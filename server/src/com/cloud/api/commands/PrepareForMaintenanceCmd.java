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

import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;

@Implementation(method="maintain", manager=Manager.AgentManager)
public class PrepareForMaintenanceCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(PrepareForMaintenanceCmd.class.getName());	
    private static final String s_name = "preparehostformaintenanceresponse";

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
    	return "host";
    }
    
//    @Override
//    public List<Pair<String, Object>> execute(Map<String, Object> params) {
//        Long hostId = (Long)params.get(BaseCmd.Properties.ID.getName());
//        
//        //verify input parameters
//    	HostVO host = getManagementServer().getHostBy(hostId);
//    	if (host == null) {
//    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "Host with id " + hostId.toString() + " doesn't exist");
//    	}
//        
//    	long jobId = 0;
//    	try {
//    		jobId = getManagementServer().prepareForMaintenanceAsync(hostId);
//    	} catch (InvalidParameterValueException e) {
//    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to prepare host for maintenance: " + e.getMessage());
//    	}
//    	
//        if(jobId == 0) {
//        	s_logger.warn("Unable to schedule async-job for PrepareForMaintenance comamnd");
//        } else {
//	        if(s_logger.isDebugEnabled())
//	        	s_logger.debug("PrepareForMaintenance command has been accepted, job id: " + jobId);
//        }
//        
//        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
//        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), Long.valueOf(jobId))); 
//        return returnValues;
//    }
    
	@Override
	public String getResponse() {
		// TODO Auto-generated method stub
		return null;
	}
}
