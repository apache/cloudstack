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
import com.cloud.vm.InstanceGroupVO;

public class DeleteVMGroupCmd extends BaseCmd{
    public static final Logger s_logger = Logger.getLogger(DeleteVMGroupCmd.class.getName());
    private static final String s_name = "deleteinstancegroupresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
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
        Long groupId = (Long)params.get(BaseCmd.Properties.ID.getName());
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        
        // Verify input parameters
        InstanceGroupVO group = getManagementServer().findVmGroupById(groupId.longValue());
        if (group == null) {
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find a vm group with id " + groupId);
        }
        
        if (account != null) {
        	Account tempAccount = getManagementServer().findAccountById(group.getAccountId());
            if (!isAdmin(account.getType()) && (account.getId() != group.getAccountId())) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find a group with id " + groupId);
            } else if (!getManagementServer().isChildDomain(account.getDomainId(), tempAccount.getDomainId())) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid group id (" + groupId + ") given, unable to update the group.");
            }
        }
        
    	boolean success = false;
        try {     
            success = getManagementServer().deleteVmGroup(groupId);
        } catch (Exception ex) {
            s_logger.error("Exception deleting vm group", ex);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to delete vm group " + groupId + ":  internal error.");
        }

        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        if (success) {
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.SUCCESS.getName(), Boolean.TRUE));
        } else {
        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to delete vm group " + groupId);
        }
        return returnValues;
    }

}
