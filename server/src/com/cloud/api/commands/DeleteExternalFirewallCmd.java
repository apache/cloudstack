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
import com.cloud.network.ExternalNetworkDeviceManager;
import com.cloud.server.ManagementService;
import com.cloud.user.Account;
import com.cloud.utils.component.ComponentLocator;

@Implementation(description="Deletes an external firewall appliance.", responseObject = SuccessResponse.class)
public class DeleteExternalFirewallCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(DeleteExternalFirewallCmd.class.getName());	
	private static final String s_name = "deleteexternalfirewallresponse";	
	
	/////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
	
	@Parameter(name=ApiConstants.ID, type=CommandType.LONG, required = true, description="Id of the external firewall appliance.")
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
		    ExternalNetworkDeviceManager externalNetworkMgr = locator.getManager(ExternalNetworkDeviceManager.class);
			boolean result = externalNetworkMgr.deleteExternalFirewall(this);
			if (result) {
			SuccessResponse response = new SuccessResponse(getCommandName());
			response.setResponseName(getCommandName());
			this.setResponseObject(response);
			} else {
				throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to delete external firewall.");
			}
		} catch (InvalidParameterValueException e) {
			throw new ServerApiException(BaseCmd.PARAM_ERROR, "Failed to delete external firewall.");
		}
    }
}
