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
import com.cloud.storage.VolumeVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.vm.UserVmVO;

public class AttachVolumeCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(AttachVolumeCmd.class.getName());
    private static final String s_name = "attachvolumeresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.VIRTUAL_MACHINE_ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DEVICE_ID, Boolean.FALSE));
        
    }

    public String getName() {
        return s_name;
    }
    
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    public static String getResultObjectName() {
    	return "volume";
    }
    
    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
    	Account account = (Account) params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        //Long userId = (Long) params.get(BaseCmd.Properties.USER_ID.getName());
    	Long volumeId = (Long) params.get(BaseCmd.Properties.ID.getName());
    	Long vmId = (Long) params.get(BaseCmd.Properties.VIRTUAL_MACHINE_ID.getName());
        Long deviceId = (Long) params.get(BaseCmd.Properties.DEVICE_ID.getName());

    	// Check that the volume ID is valid
    	VolumeVO volume = getManagementServer().findVolumeById(volumeId);
    	if (volume == null)
    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find volume with ID: " + volumeId);

    	// Check that the virtual machine ID is valid
    	UserVmVO vm = getManagementServer().findUserVMInstanceById(vmId.longValue());
        if (vm == null) {
        	throw new ServerApiException (BaseCmd.VM_INVALID_PARAM_ERROR, "unable to find a virtual machine with id " + vmId);
        }

        // Check that the device ID is valid
        if( deviceId != null ) {
            if(deviceId.longValue() == 0) {
                throw new ServerApiException (BaseCmd.VM_INVALID_PARAM_ERROR, "deviceId can't be 0, which is used by Root device");
            } else if(deviceId.longValue() == 3) {
                throw new ServerApiException (BaseCmd.VM_INVALID_PARAM_ERROR, "deviceId can't be 3, which is used by CDROM device"); 
            }
        }
        
        if (volume.getAccountId() != vm.getAccountId()) {
        	throw new ServerApiException (BaseCmd.VM_INVALID_PARAM_ERROR, "virtual machine and volume belong to different accounts, can not attach");
        }

    	// If the account is not an admin, check that the volume and the virtual machine are owned by the account that was passed in
    	if (account != null) {
    	    if (!isAdmin(account.getType())) {
                if (account.getId().longValue() != volume.getAccountId())
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find volume with ID: " + volumeId + " for account: " + account.getAccountName());

                if (account.getId().longValue() != vm.getAccountId())
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find VM with ID: " + vmId + " for account: " + account.getAccountName());
    	    } else {
    	        if (!getManagementServer().isChildDomain(account.getDomainId(), volume.getDomainId()) ||
    	            !getManagementServer().isChildDomain(account.getDomainId(), vm.getDomainId())) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to attach volume " + volumeId + " to virtual machine instance " + vmId + ", permission denied.");
    	        }
    	    }
    	}

    	try {
    		long jobId = getManagementServer().attachVolumeToVMAsync(vmId, volumeId, deviceId);

    		if (jobId == 0) {
            	s_logger.warn("Unable to schedule async-job for AttachVolume comamnd");
            } else {
    	        if(s_logger.isDebugEnabled())
    	        	s_logger.debug("AttachVolume command has been accepted, job id: " + jobId);
            }

    		List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), Long.valueOf(jobId))); 

            return returnValues;
    	} catch (Exception ex) {
    		throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to attach volume: " + ex.getMessage());
    	}
    }
}
