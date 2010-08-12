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
import com.cloud.domain.DomainVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class DeleteDomainCmd extends BaseCmd{
    public static final Logger s_logger = Logger.getLogger(DeleteDomainCmd.class.getName());
    private static final String s_name = "deletedomainresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.CLEANUP, Boolean.FALSE));
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
        Long domainId = (Long)params.get(BaseCmd.Properties.ID.getName());
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        Boolean cleanup = (Boolean)params.get(BaseCmd.Properties.CLEANUP.getName());

        // If account is null, consider System as an owner for this action
        if (account == null) {
            account = getManagementServer().findAccountById(Long.valueOf(1L));
        }

        if ((domainId.longValue() == DomainVO.ROOT_DOMAIN) || !getManagementServer().isChildDomain(account.getDomainId(), domainId)) {
            throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to delete domain " + domainId + ", permission denied.");
        }

        // check if domain exists in the system
        DomainVO domain = getManagementServer().findDomainIdById(domainId);
    	if (domain == null) {
    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find domain " + domainId);
    	}

    	long jobId = getManagementServer().deleteDomainAsync(domainId, account.getId(), cleanup); // default owner is 'system'
    	if (jobId == 0) {
    	    s_logger.warn("Unable to schedule async-job for DeleteDomain comamnd");
    	} else {
    	    if (s_logger.isDebugEnabled())
    	        s_logger.debug("DeleteDomain command has been accepted, job id: " + jobId);
    	}

    	List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
    	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), Long.valueOf(jobId))); 
    	return returnValues;
    }
}
