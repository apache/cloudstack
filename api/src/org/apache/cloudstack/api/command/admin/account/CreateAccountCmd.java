// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.api.command.admin.account;

import java.util.Collection;
import java.util.Map;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.log4j.Logger;

import com.cloud.user.Account;
import com.cloud.user.UserAccount;
import com.cloud.user.UserContext;

@APICommand(name = "createAccount", description="Creates an account", responseObject=UserResponse.class)
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

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.UUID, entityType=DomainResponse.class,
            description="Creates the user under the specified domain.")
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

    @Parameter(name = ApiConstants.ACCOUNT_DETAILS, type = CommandType.MAP, description = "details for account used to store specific parameters")
    private Map<String, String> details;

    @Parameter(name=ApiConstants.ACCOUNT_ID, type=CommandType.STRING, description="Account UUID, required for adding account from external provisioning system")
    private String accountUUID;

    @Parameter(name=ApiConstants.USER_ID, type=CommandType.STRING, description="User UUID, required for adding account from external provisioning system")
    private String userUUID;

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

    public Map<String, String> getDetails() {
        if (details == null || details.isEmpty()) {
            return null;
        }

        Collection<String> paramsCollection = details.values();
        Map<String, String> params = (Map<String, String>) (paramsCollection.toArray())[0];
        return params;
    }

    public String getAccountUUID() {
        return accountUUID;
    }

    public String getUserUUID() {
        return userUUID;
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
        UserAccount userAccount = _accountService.createUserAccount(getUsername(), getPassword(), getFirstName(), getLastName(), getEmail(), getTimeZone(), getAccountName(), getAccountType(),
                getDomainId(), getNetworkDomain(), getDetails(), getAccountUUID(), getUserUUID());
        if (userAccount != null) {
            AccountResponse response = _responseGenerator.createUserAccountResponse(userAccount);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create a user account");
        }
    }
}
