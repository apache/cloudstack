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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.vm.UserVmVO;

public class ResetVMPasswordCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(ResetVMPasswordCmd.class.getName());
	
	private static final String s_name = "resetpasswordforvirtualmachineresponse";
	private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();
	
	static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
    }
	
	public String getName() {
		return s_name;
	}
	
	public List<Pair<Enum, Boolean>> getProperties() {
		return s_properties;
	}

	@Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
		Long vmId = (Long)params.get(BaseCmd.Properties.ID.getName());
	    Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        String password = null;

        //Verify input parameters
        UserVmVO vmInstance = getManagementServer().findUserVMInstanceById(vmId.longValue());
        if (vmInstance == null) {
        	throw new ServerApiException(BaseCmd.VM_INVALID_PARAM_ERROR, "unable to find a virtual machine with id " + vmId);
        }

        if (account != null) {
            if (!isAdmin(account.getType()) && (account.getId().longValue() != vmInstance.getAccountId())) {
                throw new ServerApiException(BaseCmd.VM_INVALID_PARAM_ERROR, "unable to find a virtual machine with id " + vmId + " for this account");
            } else if (!getManagementServer().isChildDomain(account.getDomainId(), vmInstance.getDomainId())) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid virtual machine id (" + vmId + ") given, unable to reset password.");
            }
        }

        // If command is executed via 8096 port, set userId to the id of System account (1)
        if (userId == null) {
            userId = Long.valueOf(1);
        }

    	VMTemplateVO template = getManagementServer().findTemplateById(vmInstance.getTemplateId());
    	if (template.getEnablePassword()) {
            password = getManagementServer().generateRandomPassword();
    	} else {
    		password = "saved_password";
    	}
    	
    	long jobId = getManagementServer().resetVMPasswordAsync(userId.longValue(), vmId, password);
        if(jobId == 0) {
        	s_logger.warn("Unable to schedule async-job for ResetVMPassword comamnd");
        } else {
	        if(s_logger.isDebugEnabled())
	        	s_logger.debug("ResetVMPassword command has been accepted, job id: " + jobId);
        }
    	
        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), Long.valueOf(jobId))); 
        return returnValues;
    }
}
