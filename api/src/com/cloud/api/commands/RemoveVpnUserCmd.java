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
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;
import com.cloud.event.EventTypes;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(description="Removes vpn user", responseObject=SuccessResponse.class)
public class RemoveVpnUserCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(RemoveVpnUserCmd.class.getName());

    private static final String s_name = "removevpnuserresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name=ApiConstants.USERNAME, type=CommandType.STRING, required=true, description="username for the vpn user")
    private String userName;
    
    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="an optional account for the vpn user. Must be used with domainId.")
    private String accountName;
    
    @IdentityMapper(entityTableName="projects")
    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.LONG, description="remove vpn user from the project")
    private Long projectId;

    @IdentityMapper(entityTableName="domain")
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="an optional domainId for the vpn user. If the account parameter is used, domainId must also be used.")
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

	public Long getProjecId() {
	    return projectId;
	}

	
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

	@Override
    public String getCommandName() {
        return s_name;
    }

	@Override
	public long getEntityOwnerId() {
	    Long accountId = getAccountId(accountName, domainId, projectId);
        if (accountId == null) {
            return UserContext.current().getCaller().getId();
        }
        
        return accountId;
    }

	@Override
	public String getEventDescription() {
		return "Remove Remote Access VPN user for account " + getEntityOwnerId() + " username= " + getUserName();
	}

	
	@Override
	public String getEventType() {
		return EventTypes.EVENT_VPN_USER_REMOVE;
	}

    @Override
    public void execute(){
        Account owner = _accountService.getAccount(getEntityOwnerId());
        boolean result = _ravService.removeVpnUser(owner.getId(), userName);
        if (!result) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to remove vpn user");
        }
        
        if (!_ravService.applyVpnUsers(owner.getId())) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to apply vpn user removal");
        } 
        SuccessResponse response = new SuccessResponse(getCommandName());
        setResponseObject(response);
    }
}
