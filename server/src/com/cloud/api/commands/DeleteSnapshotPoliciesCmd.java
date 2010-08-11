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
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.server.ManagementServer;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.VolumeVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class DeleteSnapshotPoliciesCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteSnapshotPoliciesCmd.class.getName());

    private static final String s_name = "deletesnapshotpoliciesresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.IDS, Boolean.FALSE));
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
    	Long policyId = (Long)params.get(BaseCmd.Properties.ID.getName());
        String policyIds = (String)params.get(BaseCmd.Properties.IDS.getName());
        Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());

        // If command is executed via 8096 port, set userId to the id of System account (1)
        if (userId == null) {
            userId = Long.valueOf(1);
        }

        if ((policyId == null) && (policyIds == null)) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "No policy id (or list off ids) specified.");
        }

        List<Long> policyIdList = new ArrayList<Long>();

        if (policyId != null) {
        	policyIdList.add(policyId);
        } else if (policyIds != null) {
        	StringTokenizer st = new StringTokenizer(policyIds, ",");
        	while (st.hasMoreTokens()) {
        		String token = st.nextToken();
        		try {
        			Long nextId = Long.parseLong(token);
        			policyIdList.add(nextId);
        		} catch (NumberFormatException nfe) {
        			throw new ServerApiException(BaseCmd.PARAM_ERROR, "The policy id " + token + " is not a valid parameter.");
        		}
        	}
        }
        
        ManagementServer managementServer = getManagementServer();
        for (Long policy : policyIdList) {
            SnapshotPolicyVO snapshotPolicyVO = managementServer.findSnapshotPolicyById(policy);
            if (snapshotPolicyVO == null) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Policy id given: " + policy + " does not exist");
            }
            VolumeVO volume = managementServer.findVolumeById(snapshotPolicyVO.getVolumeId());
            if (volume == null) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Policy id given: " + policy + " does not belong to a valid volume");
            }
            
            // If an account was passed in, make sure that it matches the account of the volume
            checkAccountPermissions(params, volume.getAccountId(), volume.getDomainId(), "volume", volume.getId());
        }
        
        try {
            boolean success = true;
            if (policyIdList.size() > 0) {
                success = managementServer.deleteSnapshotPolicies(userId, policyIdList);
            }
    
            List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.SUCCESS.getName(), Boolean.valueOf(success).toString()));
            return returnValues;
            
        } catch (InvalidParameterValueException ex) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Error deleting snapshot policy: " + ex.getMessage());
        }
    }
}
