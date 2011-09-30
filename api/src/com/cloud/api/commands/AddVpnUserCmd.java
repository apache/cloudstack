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
import com.cloud.api.BaseAsyncCreateCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.VpnUsersResponse;
import com.cloud.domain.Domain;
import com.cloud.event.EventTypes;
import com.cloud.network.VpnUser;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(description="Adds vpn users", responseObject=VpnUsersResponse.class)
public class AddVpnUserCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(AddVpnUserCmd.class.getName());

    private static final String s_name = "addvpnuserresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name=ApiConstants.USERNAME, type=CommandType.STRING, required=true, description="username for the vpn user")
    private String userName;
    
    @Parameter(name=ApiConstants.PASSWORD, type=CommandType.STRING, required=true, description="password for the username")
    private String password;
    
    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="an optional account for the vpn user. Must be used with domainId.")
    private String accountName;
    
    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.LONG, description="add vpn user to the specific project")
    private Long projectId;

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
	
	public String getPassword() {
		return password;
	}
	
	public Long getProjectId() {
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
		return "Add Remote Access VPN user for account " + getEntityOwnerId() + " username= " + getUserName();
	}

	@Override
	public String getEventType() {
		return EventTypes.EVENT_VPN_USER_ADD;
	}

    @Override
    public void execute(){
            VpnUser vpnUser = _entityMgr.findById(VpnUser.class, getEntityId());
            Account account = _entityMgr.findById(Account.class, vpnUser.getAccountId());
            if (!_ravService.applyVpnUsers(vpnUser.getAccountId())) {
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to add vpn user");
            }
            
            VpnUsersResponse vpnResponse = new VpnUsersResponse();
            vpnResponse.setId(vpnUser.getId());
            vpnResponse.setUserName(vpnUser.getUsername());
            vpnResponse.setAccountName(account.getAccountName());
            
            vpnResponse.setDomainId(account.getDomainId());
            vpnResponse.setDomainName(_entityMgr.findById(Domain.class, account.getDomainId()).getName());
            
            vpnResponse.setResponseName(getCommandName());
            vpnResponse.setObjectName("vpnuser");
            this.setResponseObject(vpnResponse);
    }

    @Override
    public void create() {
        Account owner = _accountService.getAccount(getEntityOwnerId());
     
        VpnUser vpnUser = _ravService.addVpnUser(owner.getId(), userName, password);
        if (vpnUser == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to add vpn user");
        }
        setEntityId(vpnUser.getId());
    }	
}
