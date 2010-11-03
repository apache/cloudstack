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

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.RemoteAccessVpnResponse;
import com.cloud.api.response.SuccessResponse;
import com.cloud.event.EventTypes;
import com.cloud.network.NetworkManager;
import com.cloud.network.RemoteAccessVpnVO;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation( method="removeVpnUser", manager=NetworkManager.class, description="Removes vpn user")
public class RemoveVpnUserCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(RemoveVpnUserCmd.class.getName());

    private static final String s_name = "removevpnuserresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name="username", type=CommandType.STRING, required=true, description="username for the vpn user")
    private String userName;
    
    @Parameter(name="account", type=CommandType.STRING, description="an optional account for the vpn user. Must be used with domainId.")
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG, description="an optional domainId for the vpn user. If the account parameter is used, domainId must also be used.")
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

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

   

	public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public SuccessResponse getResponse() {
    	Boolean success = (Boolean)getResponseObject();
        SuccessResponse response = new SuccessResponse();
        response.setSuccess(success);
        response.setResponseName(getName());
        return response;
    }

	@Override
	public long getAccountId() {
		Account account = (Account)UserContext.current().getAccount();
        if ((account == null) || isAdmin(account.getType())) {
            if ((domainId != null) && (accountName != null)) {
                Account userAccount = ApiDBUtils.findAccountByNameDomain(accountName, domainId);
                if (userAccount != null) {
                    return userAccount.getId();
                }
            }
        }

        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

	@Override
	public String getEventDescription() {
		return "Remove Remote Access VPN user for account " + getAccountId() + " username= " + getUserName();
	}

	
	@Override
	public String getEventType() {
		return EventTypes.EVENT_VPN_USER_REMOVE;
	}


	
}
