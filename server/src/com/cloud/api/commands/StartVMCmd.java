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
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;

@Implementation(method="startVirtualMachine", manager=Manager.UserVmManager)
public class StartVMCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(StartVMCmd.class.getName());

    private static final String s_name = "startvirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true)
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
    	return "virtualmachine";
    }

//    @Override
//    public List<Pair<String, Object>> execute(Map<String, Object> params) {
//        Long vmId = (Long)params.get(BaseCmd.Properties.ID.getName());
//        Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
//        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
//        
//        //if account is removed, return error
//        if(account!=null && account.getRemoved() != null)
//        	throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "The account " + account.getId()+" is removed");
//        
//        //Verify input parameters
//        UserVmVO vmInstanceCheck = getManagementServer().findUserVMInstanceById(vmId.longValue());
//        if (vmInstanceCheck == null) {
//        	throw new ServerApiException (BaseCmd.VM_INVALID_PARAM_ERROR, "unable to find a virtual machine with id " + vmId);
//        }
//
//        if (account != null) {
//            if (!isAdmin(account.getType()) && (account.getId().longValue() != vmInstanceCheck.getAccountId())) {
//                throw new ServerApiException(BaseCmd.VM_INVALID_PARAM_ERROR, "unable to find a virtual machine with id " + vmId + " for this account");
//            } else if (!getManagementServer().isChildDomain(account.getDomainId(), vmInstanceCheck.getDomainId())) {
//                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid virtual machine id (" + vmId + ") given, unable to start virtual machine.");
//            }
//        }
//
//        if (userId == null) {
//            userId = Long.valueOf(1);
//        }
//
//        long jobId = getManagementServer().startVirtualMachineAsync(userId.longValue(), vmId.longValue(), null);
//        if(jobId == 0) {
//        	s_logger.warn("Unable to schedule async-job for StartVM comamnd");
//        } else {
//	        if(s_logger.isDebugEnabled())
//	        	s_logger.debug("StartVM command has been accepted, job id: " + jobId);
//        }
//        
//        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
//        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), Long.valueOf(jobId))); 
//        
//        return returnValues;
//    }

	@Override
	public String getResponse() {
		// TODO Add the response object as per executor
		return null;
	}
}
