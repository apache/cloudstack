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
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(description="Destroys a l2tp/ipsec remote access vpn", responseObject=SuccessResponse.class)
public class DeleteRemoteAccessVpnCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteRemoteAccessVpnCmd.class.getName());

    private static final String s_name = "deleteremoteaccessvpnresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name="zoneid", type=CommandType.LONG, required=true, description="zone id where the vpn server needs to be created")
    private Long zoneId;
    
    @Parameter(name="account", type=CommandType.STRING, description="an optional account for the virtual machine. Must be used with domainId.")
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG, description="an optional domainId for the virtual machine. If the account parameter is used, domainId must also be used.")
    private Long domainId;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
	
	public void setZoneId(Long zoneId) {
		this.zoneId = zoneId;
	}

	public Long getZoneId() {
		return zoneId;
	}

	   
	public String getAccountName() {
		return accountName;
	}

	public Long getDomainId() {
		return domainId;
	}
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

	public String getName() {
        return s_name;
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
		return "Delete Remote Access VPN for account " + getAccountId() + " in zone " + getZoneId();
	}

	@Override
	public String getEventType() {
		return EventTypes.EVENT_REMOTE_ACCESS_VPN_DESTROY;
	}

    @Override
    public void execute() throws ServerApiException, InvalidParameterValueException, PermissionDeniedException, InsufficientAddressCapacityException, InsufficientCapacityException, ConcurrentOperationException{
        boolean result = _networkMgr.destroyRemoteAccessVpn(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to delete remote access vpn");
        }
    }
	
}
