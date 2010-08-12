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
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;

public class AssignPortForwardingServiceCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(AssignPortForwardingServiceCmd.class.getName());
	
    private static final String s_name = "assignportforwardingserviceresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.IDS, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PUBLIC_IP, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.VIRTUAL_MACHINE_ID, Boolean.TRUE));
    }

    public String getName() {
        return s_name;
    }
    
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
        Long securityGroupId = (Long)params.get(BaseCmd.Properties.ID.getName());
        String securityGroupIds = (String)params.get(BaseCmd.Properties.IDS.getName());
        String publicIp = (String)params.get(BaseCmd.Properties.PUBLIC_IP.getName());
        Long vmId = (Long)params.get(BaseCmd.Properties.VIRTUAL_MACHINE_ID.getName());

        if ((securityGroupId == null) && (securityGroupIds == null)) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "No service id (or list of ids) specified.");
        }

        List<Long> sgIdList = null;
        if (securityGroupIds != null) {
            sgIdList = new ArrayList<Long>();
            StringTokenizer st = new StringTokenizer(securityGroupIds, ",");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                try {
                    Long nextSGId = Long.parseLong(token);
                    sgIdList.add(nextSGId);
                } catch (NumberFormatException nfe) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "The service id " + token + " is not a valid parameter.");
                }
            }
        }

        if (userId == null) {
            userId = Long.valueOf(1);
        }

        List<Long> validateSGList = null;
        if (securityGroupId == null) {
            validateSGList = sgIdList;
        } else {
            validateSGList = new ArrayList<Long>();
            validateSGList.add(securityGroupId);
        }
        Long validatedAccountId = getManagementServer().validateSecurityGroupsAndInstance(validateSGList, vmId);
        if (validatedAccountId == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to apply port forwarding services " + StringUtils.join(sgIdList, ",") + " to instance " + vmId + ".  Invalid list of port forwarding services for the given instance.");
        }
        if (account != null) {
            if (!isAdmin(account.getType()) && (account.getId().longValue() != validatedAccountId.longValue())) {
                throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Permission denied applying port forwarding services " + StringUtils.join(sgIdList, ",") + " to instance " + vmId + ".");
            } else {
                Account validatedAccount = getManagementServer().findAccountById(validatedAccountId);
                if (!getManagementServer().isChildDomain(account.getDomainId(), validatedAccount.getDomainId())) {
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Permission denied applying port forwarding services " + StringUtils.join(sgIdList, ",") + " to instance " + vmId + ".");
                }
            }
        }

        long jobId = getManagementServer().assignSecurityGroupAsync(userId, securityGroupId, sgIdList, publicIp, vmId);
        
        if(jobId == 0) {
        	s_logger.warn("Unable to schedule async-job for AssignPortForwardingServiceCmd comamnd");
        } else {
	        if(s_logger.isDebugEnabled())
	        	s_logger.debug("AssignPortForwardingServiceCmd command has been accepted, job id: " + jobId);
        }
        
        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), Long.valueOf(jobId))); 
        return returnValues;
    }
}
