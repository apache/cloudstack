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

public class UpdateVMCmd extends BaseCmd{
    public static final Logger s_logger = Logger.getLogger(UpdateVMCmd.class.getName());
    private static final String s_name = "updatevirtualmachineresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.GROUP, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DISPLAY_NAME, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.HA_ENABLE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
    }

    @Override
    public String getName() {
        return s_name;
    }
    @Override
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
    	Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        Long vmId = (Long)params.get(BaseCmd.Properties.ID.getName());
        String group = (String)params.get(BaseCmd.Properties.GROUP.getName());
        String displayName = (String)params.get(BaseCmd.Properties.DISPLAY_NAME.getName());
        Boolean enable = (Boolean)params.get(BaseCmd.Properties.HA_ENABLE.getName());
        UserVmVO vmInstance = null;

        // default userId to SYSTEM user
        if (userId == null) {
            userId = Long.valueOf(1);
        }

        // Verify input parameters
        try {
        	vmInstance = getManagementServer().findUserVMInstanceById(vmId.longValue());
        } catch (Exception ex1) {
        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "unable to find virtual machine by id");
        }

        if (vmInstance == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find virtual machine with id " + vmId);
        }

        if (account != null) {
            if (!isAdmin(account.getType()) && (account.getId().longValue() != vmInstance.getAccountId())) {
                throw new ServerApiException(BaseCmd.VM_INVALID_PARAM_ERROR, "unable to find a virtual machine with id " + vmId + " for this account");
            } else if (!getManagementServer().isChildDomain(account.getDomainId(), vmInstance.getDomainId())) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid virtual machine id (" + vmId + ") given, unable to update virtual machine.");
            }
        }

        if (group == null) {
    		group = vmInstance.getGroup();
    	}

    	if (displayName == null) {
    		displayName = vmInstance.getDisplayName();
    	}
    	
    	if (enable == null) {
    		enable = vmInstance.isHaEnabled();
    	}

    	long accountId = vmInstance.getAccountId();

        try {     
        	getManagementServer().updateVirtualMachine(vmId, displayName, group, enable, userId, accountId);
        } catch (Exception ex) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update virtual machine" + vmId + ":  internal error.");
        }

        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.SUCCESS.getName(), Boolean.TRUE));
        return returnValues;
    }  
}
