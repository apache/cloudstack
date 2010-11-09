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

import com.cloud.api.ApiConstants;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.UserVmResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;

@Implementation(description="Resets the password for virtual machine. " +
																					"The virtual machine must be in a \"Stopped\" state and the template must already " +
																					"support this feature for this command to take effect. [async]")
public class ResetVMPasswordCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(ResetVMPasswordCmd.class.getName());

	private static final String s_name = "resetpasswordforvirtualmachineresponse";
	
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="The ID of the virtual machine")
    private Long id;

    // unexposed parameter needed for serializing/deserializing the command
    @Parameter(name=ApiConstants.PASSWORD, type=CommandType.STRING, expose=false)
    private String password;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
	public String getName() {
		return s_name;
	}

    @Override
    public long getAccountId() {
        UserVm vm = ApiDBUtils.findUserVmById(getId());
        if (vm != null) {
            return vm.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VM_RESETPASSWORD;
    }

    @Override
    public String getEventDescription() {
        return  "resetting password for vm: " + getId();
    }
	
    @Override
    public void execute() throws ServerApiException, InvalidParameterValueException, PermissionDeniedException, InsufficientAddressCapacityException, InsufficientCapacityException, ConcurrentOperationException, StorageUnavailableException{
        UserVm result = _userVmService.resetVMPassword(this);
        UserVmResponse response = ApiResponseHelper.createUserVmResponse(result);
        
        // FIXME:  where will the password come from in this case?
//        if (templatePasswordEnabled) {
//            response.setPassword(getPassword());
//        } 
        response.setResponseName(getName());
        this.setResponseObject(response);
    }
}
