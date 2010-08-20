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

import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.BaseCmd.Manager;

@Implementation(method="recoverVirtualMachine", manager=Manager.UserVmManager)
public class RecoverVMCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(RecoverVMCmd.class.getName());

    private static final String s_name = "recovervirtualmachineresponse";

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

	@Override
	public String getResponse() {
		// TODO Auto-generated method stub
		return null;
	}

//    @Override
//    public List<Pair<String, Object>> execute(Map<String, Object> params) {
//        Long vmId = (Long)params.get(BaseCmd.Properties.ID.getName());
//        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
//   
//        //if account is removed, return error
//        if(account!=null && account.getRemoved() != null)
//        	throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "The account " + account.getId()+" is removed");
//        	
//        // Verify input parameters
//        UserVmVO vmInstance = getManagementServer().findUserVMInstanceById(vmId.longValue());
//        if (vmInstance == null) {
//        	throw new ServerApiException(BaseCmd.VM_INVALID_PARAM_ERROR, "unable to find a virtual machine with id " + vmId);
//        }
//
//        if ((account != null) && !getManagementServer().isChildDomain(account.getDomainId(), vmInstance.getDomainId())) {
//            // the domain in which the VM lives is not in the admin's domain tree
//            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to recover virtual machine with id " + vmId + ", invalid id given.");
//        }
//
//        try {
//            boolean success = getManagementServer().recoverVirtualMachine(vmId.longValue());
//            if (success == false) {
//                throw new ServerApiException(BaseCmd.VM_RECOVER_ERROR, "unable to recover virtual machine with id " + vmId.toString());
//            }
//            List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
//            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.SUCCESS.getName(), Boolean.valueOf(success).toString()));
//            return returnValues;
//        } catch (ResourceAllocationException ex) {
//            throw new ServerApiException(BaseCmd.VM_RECOVER_ERROR, "Failed to recover virtual machine with id " + vmId + "; " + ex.getMessage());
//        } catch (InternalErrorException e) {
//        	throw new ServerApiException(BaseCmd.VM_RECOVER_ERROR, "Failed to recover virtual machine with id " + vmId + "; " + e.getMessage());
//		}
//    }
}
