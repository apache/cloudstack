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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.BaseCmd.CommandType;
import com.cloud.api.response.AccountResponse;
import com.cloud.user.Account;

@Implementation(description="Updates account information for the authenticated user", responseObject=AccountResponse.class)
public class UpdateAccountCmd extends BaseCmd{
    public static final Logger s_logger = Logger.getLogger(UpdateAccountCmd.class.getName());
    private static final String s_name = "updateaccountresponse";
 
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, required=true, description="the current account name")
    private String accountName;

    @IdentityMapper(entityTableName="domain")
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, required=true, description="the ID of the domain where the account exists")
    private Long domainId;

    @Parameter(name=ApiConstants.NEW_NAME, type=CommandType.STRING, required=true, description="new name for the account")
    private String newName;
    
    @Parameter(name=ApiConstants.NETWORK_DOMAIN, type=CommandType.STRING, description="Network domain for the account's networks")
    private String networkDomain;
    
	@Parameter(name = ApiConstants.ACCOUNT_DETAILS, type = CommandType.MAP, description = "details for account used to store specific parameters")
    private Map details;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getNewName() {
        return newName;
    }
    
    public String getNetworkDomain() {
        return networkDomain;
    }
    
    public Map getDetails() {
    	if (details == null || details.isEmpty()) {
    		return null;
    	}
    	
    	Collection paramsCollection = details.values();
    	Map params = (Map) (paramsCollection.toArray())[0];
    	return params;
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
        Account account = _accountService.getActiveAccountByName(getAccountName(), getDomainId());
        if (account != null) {
            return account.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }
    
    @Override
    public void execute(){
        Account result = _accountService.updateAccount(this);
        if (result != null){
            AccountResponse response = _responseGenerator.createAccountResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update account");
        }
    }
}
