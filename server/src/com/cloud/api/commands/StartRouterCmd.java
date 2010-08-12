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
import com.cloud.vm.DomainRouterVO;

public class StartRouterCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(StartRouterCmd.class.getName());

    private static final String s_name = "startrouterresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
    }

    public String getName() {
        return s_name;
    }
    
    public static String getResultObjectName() {
    	return "router"; 
    }
    
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    public List<Pair<String, Object>> execute(Map<String, Object> params) {
	    Long routerId = (Long)params.get(BaseCmd.Properties.ID.getName());
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());

	    //verify parameters
        DomainRouterVO router = getManagementServer().findDomainRouterById(routerId);
        if (router == null) {
        	throw new ServerApiException (BaseCmd.PARAM_ERROR, "unable to find a domain router with id " + routerId);
        }
        if ((account != null) && !getManagementServer().isChildDomain(account.getDomainId(), router.getDomainId())) {
            throw new ServerApiException (BaseCmd.PARAM_ERROR, "Invalid domain router id (" + routerId + ") given, unable to start router.");
        }

        long jobId = getManagementServer().startRouterAsync(routerId.longValue());
        if (jobId == 0) {
        	s_logger.warn("Unable to schedule async-job for StartRouter comamnd");
        } else {
	        if(s_logger.isDebugEnabled())
	        	s_logger.debug("StartRouter command has been accepted, job id: " + jobId);
        }
	    
	    List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
	    returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), Long.valueOf(jobId))); 
	    return returnValues;
    }
}
