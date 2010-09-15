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

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;

@Implementation(method="disableAccount", manager=Manager.ManagementServer)
public class DisableAccountCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(DisableAccountCmd.class.getName());
    private static final String s_name = "disableaccountresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING)
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG)
    private Long domainId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override
    public String getResponse() {
        // TODO:  implement
        return null;
    }

//    @Override
//    public List<Pair<String, Object>> execute(Map<String, Object> params) {
//        Account adminAccount = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
//        Long domainId = (Long)params.get(BaseCmd.Properties.DOMAIN_ID.getName());
//        String accountName = (String)params.get(BaseCmd.Properties.ACCOUNT.getName());
//
//        if ((adminAccount != null) && !getManagementServer().isChildDomain(adminAccount.getDomainId(), domainId)) {
//            throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Failed to disable account " + accountName + " in domain " + domainId + ", permission denied.");
//        }
//
//        Account account = getManagementServer().findActiveAccount(accountName, domainId);
//        if (account == null) {
//            throw new ServerApiException (BaseCmd.PARAM_ERROR, "Unable to find active account with name " + accountName + " in domain " + domainId);
//        }
//
//        // don't allow modify system account
//        if (account.getId().longValue() == Account.ACCOUNT_ID_SYSTEM) {
//            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "can not disable system account");
//        }
//
//        long jobId = getManagementServer().disableAccountAsync(account.getId().longValue());
//        if (jobId == 0) {
//        	s_logger.warn("Unable to schedule async-job for DisableAccount comamnd");
//        } else {
//	        if (s_logger.isDebugEnabled())
//	        	s_logger.debug("DisableAccount command has been accepted, job id: " + jobId);
//        }
//
//        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
//        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), Long.valueOf(jobId))); 
//        return returnValues;
//    }
}
