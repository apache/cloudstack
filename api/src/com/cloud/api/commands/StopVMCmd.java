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
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.UserVmResponse;
import com.cloud.async.AsyncJob;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.uservm.UserVm;

@Implementation(responseObject=UserVmResponse.class, description="Stops a virtual machine.")
public class StopVMCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(StopVMCmd.class.getName());

    private static final String s_name = "stopvirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="vm_instance")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="The ID of the virtual machine")
    private Long id;
    
    @Parameter(name=ApiConstants.FORCED, type=CommandType.BOOLEAN, required=false, description="Force stop the VM.  The caller knows the VM is stopped.")
    private Boolean forced;


    /////////////////////////////////////////////////////
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
    
    public static String getResultObjectName() {
        return "virtualmachine";
    }

    @Override
    public long getEntityOwnerId() {
        UserVm vm = _responseGenerator.findUserVmById(getId());
        if (vm != null) {
            return vm.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VM_STOP;
    }

    @Override
    public String getEventDescription() {
        return  "stopping user vm: " + getId();
    }
    
    @Override
    public AsyncJob.Type getInstanceType() {
    	return AsyncJob.Type.VirtualMachine;
    }
    
    @Override
    public Long getInstanceId() {
    	return getId();
    }
    
    public boolean isForced() {
        return (forced != null) ? forced : false;
    }

    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException{
        UserContext.current().setEventDetails("Vm Id: "+getId());
        UserVm result;
        
        if (_userVmService.getHypervisorTypeOfUserVM(getId()) == HypervisorType.BareMetal) {
        	result = _bareMetalVmService.stopVirtualMachine(getId(), isForced());
        } else {
        	result = _userVmService.stopVirtualMachine(getId(), isForced());
        }
        
        if (result != null) {
            UserVmResponse response = _responseGenerator.createUserVmResponse("virtualmachine", result).get(0);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to stop vm");
        }
    }
}
