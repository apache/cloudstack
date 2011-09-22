/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
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
 *@author-aj
 */
package com.cloud.api.commands.netapp;

import java.rmi.ServerException;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.netapp.NetappManager;
import com.cloud.server.ManagementService;
import com.cloud.server.api.response.netapp.CreateLunCmdResponse;
import com.cloud.utils.component.ComponentLocator;

@Implementation(description="Create a LUN from a pool", responseObject = CreateLunCmdResponse.class)
public class CreateLunCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(CreateLunCmd.class.getName());
    private static final String s_name = "createlunresponse";
    
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    
    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required = true, description="pool name.")
	private String poolName;
    
    @Parameter(name=ApiConstants.SIZE, type=CommandType.LONG, required = true, description="LUN size.")
    private long size;
    
    public String getPoolName() {
    	return poolName;
    }
    
    public long getLunSize() {
    	return size;
    }

	@Override
	public void execute() throws ResourceUnavailableException,
			InsufficientCapacityException, ServerApiException,
			ConcurrentOperationException, ResourceAllocationException {
		ComponentLocator locator = ComponentLocator.getLocator(ManagementService.Name);
    	NetappManager netappMgr = locator.getManager(NetappManager.class);
    	
    	try {
    		CreateLunCmdResponse response = new CreateLunCmdResponse();
    		String returnVals[] = null;
    		returnVals = netappMgr.createLunOnFiler(getPoolName(), getLunSize());
    		response.setPath(returnVals[0]);
    		response.setIqn(returnVals[1]);
    		response.setIpAddress(returnVals[2]);
    		response.setObjectName("lun");
    		response.setResponseName(getCommandName());
    		this.setResponseObject(response);
    	} catch (ServerException e) {
    		throw new ServerApiException(BaseCmd.PARAM_ERROR, e.toString());
    	} catch (InvalidParameterValueException e) {
    		throw new ServerApiException(BaseCmd.INTERNAL_ERROR, e.toString());
    	}
		
	}

	@Override
	public String getCommandName() {
		// TODO Auto-generated method stub
		return s_name;
	}

	@Override
	public long getEntityOwnerId() {
		// TODO Auto-generated method stub
		return 0;
	}
    
}
