/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.NetworkUsageManager;
import com.cloud.server.ManagementService;
import com.cloud.user.Account;
import com.cloud.utils.component.ComponentLocator;

@Implementation(description="Deletes an traffic monitor host.", responseObject = SuccessResponse.class)
public class DeleteTrafficMonitorCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(DeleteTrafficMonitorCmd.class.getName());	
	private static final String s_name = "deletetrafficmonitorresponse";	
	
	/////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
	
	@Parameter(name=ApiConstants.ID, type=CommandType.LONG, required = true, description="Id of the Traffic Monitor Host.")
	private Long id;
	
	///////////////////////////////////////////////////
	/////////////////// Accessors ///////////////////////
	/////////////////////////////////////////////////////
	 
	public Long getId() {
		return id;
	}
	 
	/////////////////////////////////////////////////////
	/////////////// API Implementation///////////////////
	/////////////////////////////////////////////////////

	@Override
	public String getCommandName() {
		return s_name;
	}
	
	@Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
	 
	@Override
    public void execute(){
		try {
		    ComponentLocator locator = ComponentLocator.getLocator(ManagementService.Name);
		    NetworkUsageManager _networkUsageMgr = locator.getManager(NetworkUsageManager.class);
			boolean result = _networkUsageMgr.deleteTrafficMonitor(this);
			if (result) {
			SuccessResponse response = new SuccessResponse(getCommandName());
			response.setResponseName(getCommandName());
			this.setResponseObject(response);
			} else {
				throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to delete traffic monitor.");
			}
		} catch (InvalidParameterValueException e) {
			throw new ServerApiException(BaseCmd.PARAM_ERROR, "Failed to delete traffic monitor.");
		}
    }
}
