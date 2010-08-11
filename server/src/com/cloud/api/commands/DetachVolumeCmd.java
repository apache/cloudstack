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

public class DetachVolumeCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(DetachVolumeCmd.class.getName());
    private static final String s_name = "detachvolumeresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.TRUE));
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
    	Long volumeId = (Long) params.get(BaseCmd.Properties.ID.getName());
    	
    	boolean isAdmin;
    	if (account == null) {
    		// Admin API call
    		isAdmin = true;
    	} else {
    		// User API call
    		isAdmin = isAdmin(account.getType());
    	}

    	// Check that the volume ID is valid
    	VolumeVO volume = getManagementServer().findVolumeById(volumeId);
    	if (volume == null)
    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find volume with ID: " + volumeId);

    	// If the account is not an admin, check that the volume is owned by the account that was passed in
    	if (!isAdmin) {
    		if (account.getId() != volume.getAccountId())
    			throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find volume with ID: " + volumeId + " for account: " + account.getAccountName());
    	} else if (account != null) {
    	    if (!getManagementServer().isChildDomain(account.getDomainId(), volume.getDomainId())) {
                throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to detach volume with ID: " + volumeId + ", permission denied.");
    	    }
    	}

    	try {
    		long jobId = getManagementServer().detachVolumeFromVMAsync(volumeId);

    		if (jobId == 0) {
            	s_logger.warn("Unable to schedule async-job for DetachVolume comamnd");
            } else {
    	        if(s_logger.isDebugEnabled())
    	        	s_logger.debug("DetachVolume command has been accepted, job id: " + jobId);
            }

    		List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), Long.valueOf(jobId))); 

            return returnValues;
    	} catch (Exception ex) {
    		throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to detach volume: " + ex.getMessage());
    	}
    }
}