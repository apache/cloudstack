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
import com.cloud.async.executor.CreateOrUpdateRuleResultObject;
import com.cloud.network.SecurityGroupVO;
import com.cloud.serializer.SerializerHelper;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class CreatePortForwardingServiceRuleCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(CreatePortForwardingServiceRuleCmd.class.getName());

    private static final String s_name = "createportforwardingserviceruleresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PUBLIC_PORT, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PRIVATE_PORT, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PROTOCOL, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PORT_FORWARDING_SERVICE_ID, Boolean.TRUE));
    }

    public String getName() {
        return s_name;
    }
    
    public static String getResultObjectName() {
    	return "portforwardingservicerule";
    }
    
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
        String publicPort = (String)params.get(BaseCmd.Properties.PUBLIC_PORT.getName());
        String privatePort = (String)params.get(BaseCmd.Properties.PRIVATE_PORT.getName());
        String protocol = (String)params.get(BaseCmd.Properties.PROTOCOL.getName());
        Long securityGroupId = (Long)params.get(BaseCmd.Properties.PORT_FORWARDING_SERVICE_ID.getName());

        SecurityGroupVO sg = getManagementServer().findSecurityGroupById(securityGroupId);
        if (sg == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find port forwarding service with id " + securityGroupId);
        }

        if (account != null) {
            if (isAdmin(account.getType())) {
                if (!getManagementServer().isChildDomain(account.getDomainId(), sg.getDomainId())) {
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to find rules for port forwarding service id = " + securityGroupId + ", permission denied.");
                }
            } else if (account.getId().longValue() != sg.getAccountId().longValue()) {
                throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Invalid port forwarding service (" + securityGroupId + ") given, unable to create rule.");
            }
        }

        // If command is executed via 8096 port, set userId to the id of System account (1)
        if (userId == null) {
            userId = Long.valueOf(1);
        }

        long jobId = getManagementServer().createOrUpdateRuleAsync(true, userId.longValue(), sg.getAccountId().longValue(), null, securityGroupId, null, publicPort, null, privatePort, protocol, null);
        long ruleId = 0;

        if (jobId == 0) {
            s_logger.warn("Unable to schedule async-job for CreatePortForwardingServiceRuleCmd command");
        } else {
            if (s_logger.isDebugEnabled())
                s_logger.debug("CreatePortForwardingServiceRuleCmd command has been accepted, job id: " + jobId);
            
            ruleId = waitInstanceCreation(jobId);
        }

        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), Long.valueOf(jobId)));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.RULE_ID.getName(), Long.valueOf(ruleId))); 
        return returnValues;
    }
    
	protected long getInstanceIdFromJobSuccessResult(String result) {
		CreateOrUpdateRuleResultObject resultObject = (CreateOrUpdateRuleResultObject)SerializerHelper.fromSerializedString(result);
		if(resultObject != null) {
			return resultObject.getRuleId();
		}

		return 0;
	}
}
