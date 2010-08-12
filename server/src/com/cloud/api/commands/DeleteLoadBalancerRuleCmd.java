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
import com.cloud.network.LoadBalancerVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class DeleteLoadBalancerRuleCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteLoadBalancerRuleCmd.class.getName());

    private static final String s_name = "deleteloadbalancerruleresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.TRUE));
    }

    public String getName() {
        return s_name;
    }
    
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        Long loadBalancerId = (Long)params.get(BaseCmd.Properties.ID.getName());

        //Verify parameters
        LoadBalancerVO loadBalancer = getManagementServer().findLoadBalancerById(loadBalancerId.longValue());
        if (loadBalancer == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find load balancer rule, with id " + loadBalancerId);
        } else if (account != null) {
            if (!isAdmin(account.getType())) {
                if (loadBalancer.getAccountId() != account.getId().longValue()) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Account " + account.getAccountName() + " does not own load balancer rule " + loadBalancer.getName() + " (id:" + loadBalancerId + ")");
                }
            } else if (!getManagementServer().isChildDomain(account.getDomainId(), loadBalancer.getDomainId())) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to delete load balancer rule " + loadBalancer.getName() + " (id:" + loadBalancerId + "), permission denied.");
            }
        }

        if (userId == null) {
            userId = Long.valueOf(1);
        }

        long jobId = getManagementServer().deleteLoadBalancerAsync(userId, loadBalancerId.longValue());
        if (jobId == 0) {
        	s_logger.warn("Unable to schedule async-job for DeleteLoadBalancerRule comamnd");
        } else {
	        if (s_logger.isDebugEnabled())
	        	s_logger.debug("DeleteLoadBalancerRule command has been accepted, job id: " + jobId);
        }
        
        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), Long.valueOf(jobId))); 
        return returnValues;
    }
}
