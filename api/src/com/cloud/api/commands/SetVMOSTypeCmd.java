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
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.UserVmResponse;
import com.cloud.uservm.UserVm;

@Implementation(description="Set VM OS Type.", responseObject=UserVmResponse.class)
public class SetVMOSTypeCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(SetVMOSTypeCmd.class.getName());

    private static final String s_name = "SetVMOSTypeResponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.LONG, required=true, description="the ID of the virtual machine")
    private Long virtualMachineId;

    @Parameter(name=ApiConstants.OS_TYPE_ID, type=CommandType.LONG, required=true, description="the ID of the OS Type that best represents the OS of this template.")
    private Long osTypeId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getOsTypeId() {
        return osTypeId;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }   

   
    @Override
    public void execute(){
        boolean result = _userVmService.setVMOSType(this);
        if (result) {
            UserVm userVm = _responseGenerator.findUserVmById(virtualMachineId);
            if (userVm != null) {
                UserVmResponse response = _responseGenerator.createUserVmResponse(userVm);            
                response.setResponseName(DeployVMCmd.getResultObjectName());           
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to set VM OS type");
            }
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to set VM OS type");
        }
    }
}
