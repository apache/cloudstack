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

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.region.RegionService;
import org.apache.log4j.Logger;

import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.user.UserAccount;

@APICommand(name = "updateUser", description = "Updates a user account", responseObject = UserResponse.class,
requestHasSensitiveInfo = true, responseHasSensitiveInfo = true)
public class UpdateUserCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateUserCmd.class.getName());

    private static final String s_name = "updateuserresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.USER_API_KEY, type = CommandType.STRING, description = "The API key for the user. Must be specified with userSecretKey")
    private String apiKey;

    @Parameter(name = ApiConstants.EMAIL, type = CommandType.STRING, description = "email")
    private String email;

    @Parameter(name = ApiConstants.FIRSTNAME, type = CommandType.STRING, description = "first name")
    private String firstname;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = UserResponse.class, required = true, description = "User uuid")
    private Long id;

    @Parameter(name = ApiConstants.LASTNAME, type = CommandType.STRING, description = "last name")
    private String lastname;

    @Parameter(name = ApiConstants.PASSWORD,
            type = CommandType.STRING,
            description = "Clear text password (default hashed to SHA256SALT). If you wish to use any other hashing algorithm, you would need to write a custom authentication adapter. Can't be passed when command is executed via integration.api.port",
            acceptedOnAdminPort = false)
    private String password;

    @Parameter(name = ApiConstants.CURRENT_PASSWORD, type = CommandType.STRING, description = "Current password that was being used by the user. You must inform the current password when updating the password.", acceptedOnAdminPort = false)
    private String currentPassword;

    @Parameter(name = ApiConstants.SECRET_KEY, type = CommandType.STRING, description = "The secret key for the user. Must be specified with userApiKey")
    private String secretKey;

    @Parameter(name = ApiConstants.TIMEZONE,
            type = CommandType.STRING,
            description = "Specifies a timezone for this command. For more information on the timezone parameter, see Time Zone Format.")
    private String timezone;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, description = "Unique username")
    private String username;

    @Inject
    private RegionService _regionService;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getApiKey() {
        return apiKey;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstname() {
        return firstname;
    }

    public Long getId() {
        return id;
    }

    public String getLastname() {
        return lastname;
    }

    public String getPassword() {
        return password;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getUsername() {
        return username;
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
        User user = _entityMgr.findById(User.class, getId());
        if (user != null) {
            return user.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("UserId: " + getId());
        UserAccount user = _regionService.updateUser(this);

        if (user != null) {
            UserResponse response = _responseGenerator.createUserResponse(user);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update user");
        }
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public Long getApiResourceId() {
        return id;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.User;
    }
}
