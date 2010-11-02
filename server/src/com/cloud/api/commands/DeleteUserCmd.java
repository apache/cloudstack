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

import com.cloud.api.ApiConstants;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.SuccessResponse;
import com.cloud.event.EventTypes;
import com.cloud.server.ManagementServer;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.user.UserContext;

@Implementation(method="deleteUser", manager=ManagementServer.class, description="Deletes a user account")
public class DeleteUserCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(DeleteUserCmd.class.getName());
	private static final String s_name = "deleteuserresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="User id")
    private Long id;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

	public static String getStaticName() {
		return s_name;
	}
	
    @Override
	public String getName() {
        return s_name;
    }

    @Override
    public long getAccountId() {
        Account account = (Account)UserContext.current().getAccount();
        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_USER_DELETE;
    }

    @Override
    public String getEventDescription() {
        User user = ApiDBUtils.findUserById(getId());
        return (user != null ? ("User " + user.getUsername() + " (id: " + user.getId() + ") and accountId = " + user.getAccountId()) : "user delete, but this user does not exist in the system");
    }

	@Override @SuppressWarnings("unchecked")
	public SuccessResponse getResponse() {
        Boolean success = (Boolean)getResponseObject();
        SuccessResponse response = new SuccessResponse();
        response.setSuccess(success);
        response.setResponseName(getName());
        return response;
	}
}
