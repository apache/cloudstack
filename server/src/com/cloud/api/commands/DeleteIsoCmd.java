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

public class DeleteIsoCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteIsoCmd.class.getName());
    private static final String s_name = "deleteisosresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ZONE_ID, Boolean.FALSE));
    }

    @Override
    public String getName() {
        return s_name;
    }
    
    public static String getStaticName() {
        return s_name;
    }
    
    @Override
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Account account = (Account) params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
        Long isoId = (Long)params.get(BaseCmd.Properties.ID.getName());
        Long zoneId = (Long)params.get(BaseCmd.Properties.ZONE_ID.getName());

        if (userId == null) {
            userId = Long.valueOf(1);
        }

        VMTemplateVO iso = getManagementServer().findTemplateById(isoId);
        if (iso == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Failed to find ISO with given parameters.");
        }

        if (account != null) {
            if (!isAdmin(account.getType())) {
                if (iso.getAccountId() != account.getId().longValue()) {
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to delete ISO with id " + isoId);
                }
            } else {
                Account isoOwner = getManagementServer().findAccountById(iso.getAccountId());
                if (!getManagementServer().isChildDomain(account.getDomainId(), isoOwner.getDomainId())) {
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to delete ISO with id " + isoId + ", permission denied.");
                }
            }
        }

        try {
    		long jobId = getManagementServer().deleteIsoAsync(userId, isoId, zoneId);
    		
    		if (jobId == 0) {
            	s_logger.warn("Unable to schedule async-job for DeleteIso command");
            } else {
    	        if (s_logger.isDebugEnabled()) {
    	        	s_logger.debug("DeleteIso command has been accepted, job id: " + jobId);
    	        }
            }

    		List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), Long.valueOf(jobId))); 
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ISO_ID.getName(), Long.valueOf(isoId))); 

            return returnValues;
    	} catch (Exception ex) {
    		throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to delete template: " + ex.getMessage());
    	}
    }
}
