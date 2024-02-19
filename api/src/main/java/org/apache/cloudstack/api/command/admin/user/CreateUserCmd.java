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
package org.apache.cloudstack.api.command.admin.user;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.lang3.StringUtils;

import com.cloud.user.Account;
import com.cloud.user.User;

@APICommand(name = "createUser", description = "Creates a user for an account that already exists", responseObject = UserResponse.class,
        requestHasSensitiveInfo = true, responseHasSensitiveInfo = true)
public class CreateUserCmd extends BaseCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACCOUNT,
               type = CommandType.STRING,
               required = true,
               description = "Creates the user under the specified account. If no account is specified, the username will be used as the account name.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID,
               type = CommandType.UUID,
               entityType = DomainResponse.class,
               description = "Creates the user under the specified domain. Has to be accompanied with the account parameter")
    private Long domainId;

    @Parameter(name = ApiConstants.EMAIL, type = CommandType.STRING, required = true, description = "email")
    private String email;

    @Parameter(name = ApiConstants.FIRSTNAME, type = CommandType.STRING, required = true, description = "firstname")
    private String firstname;

    @Parameter(name = ApiConstants.LASTNAME, type = CommandType.STRING, required = true, description = "lastname")
    private String lastname;

    @Parameter(name = ApiConstants.PASSWORD,
               type = CommandType.STRING,
               required = true,
               description = "Clear text password (Default hashed to SHA256SALT). If you wish to use any other hashing algorithm, you would need to write a custom authentication adapter See Docs section.")
    private String password;

    @Parameter(name = ApiConstants.TIMEZONE,
               type = CommandType.STRING,
               description = "Specifies a timezone for this command. For more information on the timezone parameter, see Time Zone Format.")
    private String timezone;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, required = true, description = "Unique username.")
    private String username;

    @Parameter(name = ApiConstants.USER_ID, type = CommandType.STRING, description = "User UUID, required for adding account from external provisioning system")
    private String userUUID;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstname;
    }

    public String getLastName() {
        return lastname;
    }

    public String getPassword() {
        return password;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getUserName() {
        return username;
    }

    public String getUserUUID() {
        return userUUID;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        Account account = CallContext.current().getCallingAccount();
        if ((account == null) || _accountService.isAdmin(account.getId())) {
            if ((domainId != null) && (accountName != null)) {
                Account userAccount = _responseGenerator.findAccountByNameDomain(accountName, domainId);
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
    public void execute() {
        validateParams();
        CallContext.current().setEventDetails("UserName: " + getUserName() + ", FirstName :" + getFirstName() + ", LastName: " + getLastName());
        User user =
            _accountService.createUser(getUserName(), getPassword(), getFirstName(), getLastName(), getEmail(), getTimezone(), getAccountName(), getDomainId(),
                getUserUUID());
        if (user != null) {
            UserResponse response = _responseGenerator.createUserResponse(user);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create a user");
        }
    }

    /**
     * TODO: this should be done through a validator. for now replicating the validation logic in create account and user
     */
    private void validateParams() {
        if(StringUtils.isEmpty(getPassword())) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Empty passwords are not allowed");
        }
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.User;
    }
}
