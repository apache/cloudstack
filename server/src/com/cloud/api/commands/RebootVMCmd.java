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
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.BaseCmd.Manager;

@Implementation(method="rebootVirtualMachine", manager=Manager.UserVmManager)
public class RebootVMCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(RebootVMCmd.class.getName());
    private static final String s_name = "rebootvirtualmachineresponse";
   
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

//    @Override
//    public List<Pair<String, Object>> execute(Map<String, Object> params) {
//        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
//        Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
//        Long vmId = (Long)params.get(BaseCmd.Properties.ID.getName());
//        
//        //Verify input parameters
//        UserVmVO vmInstance = getManagementServer().findUserVMInstanceById(vmId.longValue());
//        if (vmInstance == null) {
//        	throw new ServerApiException(BaseCmd.VM_INVALID_PARAM_ERROR, "unable to find a virtual machine with id " + vmId);
//        }
//
//        if (account != null) {
//            if (!isAdmin(account.getType()) && (account.getId().longValue() != vmInstance.getAccountId())) {
//                throw new ServerApiException(BaseCmd.VM_INVALID_PARAM_ERROR, "unable to find a virtual machine with id " + vmId + " for this account");
//            } else if (!getManagementServer().isChildDomain(account.getDomainId(), vmInstance.getDomainId())) {
//                // the domain in which the VM lives is not in the admin's domain tree
//                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to reboot virtual machine with id " + vmId + ", invalid id given.");
//            }
//        }
//
//        // If command is executed via 8096 port, set userId to the id of System account (1)
//        if (userId == null) {
//            userId = Long.valueOf(1);
//        }
//
//        long jobId = getManagementServer().rebootVirtualMachineAsync(userId.longValue(), vmId.longValue());
//        if(jobId == 0) {
//        	s_logger.warn("Unable to schedule async-job for RebootVM comamnd");
//        } else {
//	        if(s_logger.isDebugEnabled())
//	        	s_logger.debug("RebootVM command has been accepted, job id: " + jobId);
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
