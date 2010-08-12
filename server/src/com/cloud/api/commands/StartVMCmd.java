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
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.vm.UserVmVO;

public class StartVMCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(StartVMCmd.class.getName());

    private static final String s_name = "startvirtualmachineresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
        
    }

    public String getName() {
        return s_name;
    }
    
    public static String getResultObjectName() {
    	return "virtualmachine";
    }
    
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Long vmId = (Long)params.get(BaseCmd.Properties.ID.getName());
        Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
          
        //Verify input parameters
        UserVmVO vmInstanceCheck = getManagementServer().findUserVMInstanceById(vmId.longValue());
        if (vmInstanceCheck == null) {
        	throw new ServerApiException (BaseCmd.VM_INVALID_PARAM_ERROR, "unable to find a virtual machine with id " + vmId);
        }

        if (account != null) {
            if (!isAdmin(account.getType()) && (account.getId().longValue() != vmInstanceCheck.getAccountId())) {
                throw new ServerApiException(BaseCmd.VM_INVALID_PARAM_ERROR, "unable to find a virtual machine with id " + vmId + " for this account");
            } else if (!getManagementServer().isChildDomain(account.getDomainId(), vmInstanceCheck.getDomainId())) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid virtual machine id (" + vmId + ") given, unable to start virtual machine.");
            }
        }

        if (userId == null) {
            userId = Long.valueOf(1);
        }

        long jobId = getManagementServer().startVirtualMachineAsync(userId.longValue(), vmId.longValue(), null);
        if(jobId == 0) {
        	s_logger.warn("Unable to schedule async-job for StartVM comamnd");
        } else {
	        if(s_logger.isDebugEnabled())
	        	s_logger.debug("StartVM command has been accepted, job id: " + jobId);
        }
        
        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), Long.valueOf(jobId))); 
        
        return returnValues;
    }
}
