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

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListDomainResourcesCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.AccountResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.user.Account;

@Implementation(description="Lists accounts and provides detailed account information for listed accounts", responseObject=AccountResponse.class)
public class ListAccountsCmd extends BaseListDomainResourcesCmd {
	public static final Logger s_logger = Logger.getLogger(ListAccountsCmd.class.getName());
    private static final String s_name = "listaccountsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT_TYPE, type=CommandType.LONG, description="list accounts by account type. Valid account types are 1 (admin), 2 (domain-admin), and 0 (user).")
    private Long accountType;

    @IdentityMapper(entityTableName="account")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="list account by account ID")
    private Long id;

    @Parameter(name=ApiConstants.IS_CLEANUP_REQUIRED, type=CommandType.BOOLEAN, description="list accounts by cleanuprequred attribute (values are true or false)")
    private Boolean cleanupRequired;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="list account by account name")
    private String searchName;

    @Parameter(name=ApiConstants.STATE, type=CommandType.STRING, description="list accounts by state. Valid states are enabled, disabled, and locked.")
    private String state;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getAccountType() {
        return accountType;
    }

    public Long getId() {
        return id;
    }

    public Boolean isCleanupRequired() {
        return cleanupRequired;
    }

    public String getSearchName() {
        return searchName;
    }

    public String getState() {
        return state;
    }
    

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute(){
        List<? extends Account> accounts = _accountService.searchForAccounts(this);
        ListResponse<AccountResponse> response = new ListResponse<AccountResponse>();
        List<AccountResponse> accountResponses = new ArrayList<AccountResponse>();
        for (Account account : accounts) {
            AccountResponse acctResponse = _responseGenerator.createAccountResponse(account);
            acctResponse.setObjectName("account");
            accountResponses.add(acctResponse);
        }
        response.setResponses(accountResponses);
        response.setResponseName(getCommandName());
        
        this.setResponseObject(response);
    }
}
