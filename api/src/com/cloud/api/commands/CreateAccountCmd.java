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
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.AccountResponse;
import com.cloud.api.response.UserResponse;
import com.cloud.user.Account;
import com.cloud.user.UserAccount;
import com.cloud.user.UserContext;

@Implementation(description="Creates an account", responseObject=UserResponse.class)
public class CreateAccountCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateAccountCmd.class.getName());

    private static final String s_name = "createaccountresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="Creates the user under the specified account. If no account is specified, the username will be used as the account name.")
    private String accountName;

    @Parameter(name=ApiConstants.ACCOUNT_TYPE, type=CommandType.SHORT, required=true, description="Type of the account.  Specify 0 for user, 1 for root admin, and 2 for domain admin")
    private Short accountType;

    @IdentityMapper(entityTableName="domain")
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="Creates the user under the specified domain.")
    private Long domainId;

    @Parameter(name=ApiConstants.EMAIL, type=CommandType.STRING, required=true, description="email")
    private String email;

    @Parameter(name=ApiConstants.FIRSTNAME, type=CommandType.STRING, required=true, description="firstname")
    private String firstName;

    @Parameter(name=ApiConstants.LASTNAME, type=CommandType.STRING, required=true, description="lastname")
    private String lastName;

    @Parameter(name=ApiConstants.PASSWORD, type=CommandType.STRING, required=true, description="Hashed password (Default is MD5). If you wish to use any other hashing algorithm, you would need to write a custom authentication adapter See Docs section.")
    private String password;

    @Parameter(name=ApiConstants.TIMEZONE, type=CommandType.STRING, description="Specifies a timezone for this command. For more information on the timezone parameter, see Time Zone Format.")
    private String timeZone;

    @Parameter(name=ApiConstants.USERNAME, type=CommandType.STRING, required=true, description="Unique username.")
    private String userName;
    
    @Parameter(name=ApiConstants.NETWORK_DOMAIN, type=CommandType.STRING, description="Network domain for the account's networks")
    private String networkDomain;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Short getAccountType() {
        return accountType;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPassword() {
        return password;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public String getUsername() {
        return userName;
    }
    
    public String getNetworkDomain() {
        return networkDomain;
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
        return Account.ACCOUNT_ID_SYSTEM;
    }
    
    @Override
    public void execute(){
        UserContext.current().setEventDetails("Account Name: "+getAccountName()+", Domain Id:"+getDomainId());
        UserAccount userAccount = _accountService.createUserAccount(getUsername(), getPassword(), getFirstName(), getLastName(), getEmail(), getTimeZone(), getAccountName(), getAccountType(), getDomainId(), getNetworkDomain());
        if (userAccount != null) {
            AccountResponse response = _responseGenerator.createUserAccountResponse(userAccount);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create a user account");
        }
    }
}