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
 * @author-aj
 */
package com.cloud.api.commands.netapp;

import java.rmi.ServerException;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.netapp.NetappManager;
import com.cloud.server.ManagementService;
import com.cloud.server.api.response.netapp.AssociateLunCmdResponse;
import com.cloud.utils.component.ComponentLocator;

@Implementation(description="Associate a LUN with a guest IQN", responseObject = AssociateLunCmdResponse.class)
public class AssociateLunCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(AssociateLunCmd.class.getName());
    private static final String s_name = "associatelunresponse";

	/////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    
    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required = true, description="LUN name.")
	private String lunName;
    
    @Parameter(name=ApiConstants.IQN, type=CommandType.STRING, required = true, description="Guest IQN to which the LUN associate.")
	private String guestIqn;
    
    
    ///////////////////////////////////////////////////
	/////////////////// Accessors ///////////////////////
	/////////////////////////////////////////////////////
	 
    
    public String getLunName() {
        return lunName;
    }
    
    public String getGuestIQN() {
    	return guestIqn;
    }
    
	/////////////////////////////////////////////////////
	/////////////// API Implementation///////////////////
	/////////////////////////////////////////////////////

	@Override
	public String getCommandName() {
		return s_name;
	}
	
    @Override
    public void execute(){
    	ComponentLocator locator = ComponentLocator.getLocator(ManagementService.Name);
    	NetappManager netappMgr = locator.getManager(NetappManager.class);
    	
    	try {
    		AssociateLunCmdResponse response = new AssociateLunCmdResponse();
    		String returnVals[] = null;
    		returnVals = netappMgr.associateLun(getGuestIQN(), getLunName());
    		response.setLun(returnVals[0]);
    		response.setIpAddress(returnVals[2]);
    		response.setTargetIQN(returnVals[1]);
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
	public long getEntityOwnerId() {
		// TODO Auto-generated method stub
		return 0;
	}
    
}
