/**
 *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
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
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.PlugService;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.element.CiscoNexusVSMElementService;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.event.EventTypes;

@Implementation(responseObject=SuccessResponse.class, description="disable a Cisco Nexus VSM device")
public class DisableCiscoNexusVSMCmd extends BaseAsyncCmd {

    public static final Logger s_logger = Logger.getLogger(DisableCiscoNexusVSMCmd.class.getName());
    private static final String s_name = "disablecisconexusvsmresponse";
    @PlugService CiscoNexusVSMElementService _ciscoNexusVSMService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="virtual_supervisor_module")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="Id of the Cisco Nexus 1000v VSM device to be deleted")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getCiscoNexusVSMDeviceId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
    	boolean result = _ciscoNexusVSMService.disableCiscoNexusVSM(this);
        if (result) {
        	SuccessResponse response = new SuccessResponse(getCommandName());
        	response.setResponseName(getCommandName());
        	this.setResponseObject(response);
        } else {
        	throw new ServerApiException(BaseAsyncCmd.INTERNAL_ERROR, "Failed to disable Cisco Nexus VSM device");
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventDescription() {
    	return "Disabling a Cisco Nexus VSM device";
    }

    @Override
    public String getEventType() {
    	return EventTypes.EVENT_EXTERNAL_SWITCH_MGMT_DEVICE_DISABLE;
    }    
}
